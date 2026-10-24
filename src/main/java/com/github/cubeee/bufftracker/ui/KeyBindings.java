package com.github.cubeee.bufftracker.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

final class KeyBindings {

    private KeyBindings() {
    }

    /**
     * Runs the action when the key is pressed, using the component's input and action maps.
     *
     * @param condition one of the {@code JComponent.WHEN_*} constants
     * @param name the action map key; unique per component
     */
    static void bind(JComponent component, int condition, int keyCode, String name, Runnable action) {
        component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }
}
