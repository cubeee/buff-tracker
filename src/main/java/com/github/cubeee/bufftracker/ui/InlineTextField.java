package com.github.cubeee.bufftracker.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.*;

/**
 * While idle, the field paints a JLabel instead of itself so that long text is truncated with an ellipsis at the end
 * (a JTextField would instead scroll to show the end of the text). Clicking it focuses it the usual Swing way, which
 * starts the edit.
 */
class InlineTextField extends JPanel {

    private static final Border IDLE_BORDER = new EmptyBorder(2, 2, 2, 2);
    private static final Border EDIT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(1, 1, 1, 1));

    private final JLabel label;
    private final JTextField field;
    private final String text;
    private final Predicate<String> onCommit;
    private final Consumer<Boolean> onEditingChanged;
    // Between starting the edit and committing or dropping it
    private boolean editing;

    /**
     * @param maxLength longer input, e.g. pasted text, is cut off
     * @param onEditingChanged told when editing starts and ends, so the owner can avoid removing the field mid-edit
     */
    InlineTextField(String text, Predicate<String> onCommit, int maxLength, Consumer<Boolean> onEditingChanged) {
        this.text = text;
        this.onCommit = onCommit;
        this.onEditingChanged = onEditingChanged;
        setLayout(new BorderLayout());
        setOpaque(false);

        label = new JLabel(text);
        // Text starting with <html> would otherwise be rendered as HTML, which can load remote images
        label.putClientProperty("html.disable", Boolean.TRUE);

        field = new JTextField(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (editing) {
                    super.paintComponent(graphics);
                    return;
                }
                Insets insets = getInsets();
                SwingUtilities.paintComponent(graphics, label, this, insets.left, insets.top,
                        getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
            }
        };
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new LengthFilter(maxLength));
        label.setFont(field.getFont());
        showIdle();

        addListeners();
        add(field, BorderLayout.CENTER);
    }

    private void addListeners() {
        field.addActionListener(e -> commitEdit());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                // Not editing after a temporary loss means the edit was ended meanwhile; keep it ended
                if (!editing && !event.isTemporary()) {
                    startEdit();
                }
            }

            @Override
            public void focusLost(FocusEvent event) {
                // A temporary loss is e.g. switching to another window; the field gets focus back with the client
                if (event.isTemporary()) {
                    return;
                }
                if (field.isDisplayable()) {
                    commitEdit();
                } else {
                    dropEdit();
                }
            }
        });
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                // Enter and Escape end the edit but leave the field focused, so no focus gain follows a click
                if (!editing && field.isFocusOwner()) {
                    startEdit();
                }
            }
        });
        KeyBindings.bind(field, JComponent.WHEN_FOCUSED, KeyEvent.VK_ESCAPE, "cancelEdit", this::dropEdit);
    }

    private void startEdit() {
        editing = true;
        field.setEditable(true);
        field.setOpaque(true);
        field.setBorder(EDIT_BORDER);
        field.getCaret().setVisible(true);
        field.selectAll();
        field.repaint();
        onEditingChanged.accept(true);
    }

    // Does nothing if the edit already ended, e.g. by Enter or Escape
    private void commitEdit() {
        if (!editing) {
            return;
        }
        String value = field.getText().trim();
        if (value.equals(text) || !onCommit.test(value)) {
            field.setText(text);
        } else {
            // Show the new value until the panel is rebuilt with it
            label.setText(value);
        }
        endEdit();
    }

    /**
     * Ends editing without committing: Escape, or the field was removed mid-edit, e.g. the plugin was turned off.
     */
    private void dropEdit() {
        if (!editing) {
            return;
        }
        field.setText(text);
        endEdit();
    }

    private void endEdit() {
        editing = false;
        showIdle();
        onEditingChanged.accept(false);
    }

    private void showIdle() {
        field.setEditable(false);
        field.setOpaque(false);
        field.setBorder(IDLE_BORDER);
        field.getCaret().setVisible(false);
        // Clears the selection, so the next edit starts from the beginning of the text
        field.setCaretPosition(0);
        field.repaint();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        // If focus was temporarily elsewhere, e.g. in another window, no focus loss will follow to end the edit
        dropEdit();
    }

    @Override
    public Dimension getMinimumSize() {
        // Allow shrinking below the text width so the label truncates instead of overflowing
        return new Dimension(0, super.getMinimumSize().height);
    }

    @Override
    public void setForeground(Color color) {
        super.setForeground(color);
        // Called by the JPanel constructor before the children exist
        if (field != null) {
            label.setForeground(color);
            field.setForeground(color);
        }
    }

    @Override
    public void setToolTipText(String text) {
        field.setToolTipText(text);
    }

    private static final class LengthFilter extends DocumentFilter {

        private final int maxLength;

        LengthFilter(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attributes)
                throws BadLocationException {
            replace(bypass, offset, 0, string, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String string, AttributeSet attributes)
                throws BadLocationException {
            int room = Math.max(0, maxLength - (bypass.getDocument().getLength() - length));
            String allowed = string != null && string.length() > room ? string.substring(0, room) : string;
            super.replace(bypass, offset, length, allowed, attributes);
        }
    }
}
