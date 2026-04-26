package com.github.cubeee.bufftracker.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.*;

/**
 * While idle, a JLabel is shown so that long text is truncated with an ellipsis at the end
 * (a JTextField would instead scroll to show the end of the text).
 */
class InlineTextField extends JPanel {

    private static final String IDLE = "idle";
    private static final String EDIT = "edit";

    private static final Border IDLE_BORDER = new EmptyBorder(2, 2, 2, 2);
    private static final Border EDIT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(1, 1, 1, 1));

    private final CardLayout cards = new CardLayout();
    private final JLabel label;
    private final JTextField field;
    private final String text;
    private final Consumer<Boolean> onEditingChanged;
    // Between gaining focus and losing it permanently
    private boolean editing;

    // Ends editing when the mouse is pressed anywhere outside the field. Clicking non-focusable components
    // (panels, labels) would otherwise leave the field focused.
    private final AWTEventListener clickAwayListener = event -> {
        // The source isn't always a Component, e.g. for system tray icon events
        if (event.getID() == MouseEvent.MOUSE_PRESSED
                && !(event.getSource() instanceof Component
                        && SwingUtilities.isDescendingFrom((Component) event.getSource(), getField()))) {
            stopEditing();
        }
    };

    /**
     * @param maxLength longer input, e.g. pasted text, is cut off
     * @param onEditingChanged told when editing starts and ends, so the owner can avoid removing the field mid-edit
     */
    InlineTextField(String text, Predicate<String> onCommit, int maxLength, Consumer<Boolean> onEditingChanged) {
        this.text = text;
        this.onEditingChanged = onEditingChanged;
        setLayout(cards);
        setOpaque(false);

        field = new JTextField(text);
        field.setBorder(EDIT_BORDER);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new LengthFilter(maxLength));

        label = new JLabel(text);
        // Text starting with <html> would otherwise be rendered as HTML, which can load remote images
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(field.getFont());
        label.setBorder(IDLE_BORDER);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                cards.show(InlineTextField.this, EDIT);
                field.requestFocusInWindow();
            }
        });

        field.addActionListener(e -> stopEditing());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (editing) {
                    // Focus came back after a temporary loss; keep the edit and its caret as they were
                    return;
                }
                editing = true;
                field.selectAll();
                Toolkit.getDefaultToolkit().addAWTEventListener(clickAwayListener, AWTEvent.MOUSE_EVENT_MASK);
                onEditingChanged.accept(true);
            }

            @Override
            public void focusLost(FocusEvent event) {
                // A temporary loss is e.g. switching to another window; the field gets focus back with the client.
                // Not editing means the edit was already dropped by removeNotify().
                if (event.isTemporary() || !editing) {
                    return;
                }
                if (!field.isDisplayable()) {
                    dropEdit();
                    return;
                }
                editing = false;
                Toolkit.getDefaultToolkit().removeAWTEventListener(clickAwayListener);
                String value = field.getText().trim();
                if (value.equals(text) || !onCommit.test(value)) {
                    field.setText(text);
                } else {
                    // Show the new value until the panel is rebuilt with it
                    label.setText(value);
                }
                cards.show(InlineTextField.this, IDLE);
                onEditingChanged.accept(false);
            }
        });
        field.registerKeyboardAction(e -> {
                    field.setText(text);
                    stopEditing();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);

        add(label, IDLE);
        add(field, EDIT);
        cards.show(this, IDLE);
    }

    private JTextField getField() {
        return field;
    }

    /**
     * Drops focus instead of passing it to the next component; focus loss commits the edit.
     */
    private void stopEditing() {
        if (field.isFocusOwner()) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
        }
    }

    /**
     * Ends editing without committing, for when the field is removed mid-edit, e.g. the plugin was turned off.
     */
    private void dropEdit() {
        editing = false;
        Toolkit.getDefaultToolkit().removeAWTEventListener(clickAwayListener);
        field.setText(text);
        cards.show(this, IDLE);
        onEditingChanged.accept(false);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        // If focus was temporarily elsewhere, e.g. in another window, no focus loss will follow to end the edit
        if (editing) {
            dropEdit();
        }
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
        if (label != null) {
            label.setForeground(color);
            field.setForeground(color);
        }
    }

    @Override
    public void setToolTipText(String text) {
        label.setToolTipText(text);
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
