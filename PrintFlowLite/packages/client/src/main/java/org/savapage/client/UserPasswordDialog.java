/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.client;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 *
 * A modal dialog to get the user id and password.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserPasswordDialog {

    /**
     *
     */
    private static final String[] DIALOG_BUTTON_NAMES = { "Login", "Cancel" };

    /**
     * .
     */
    private static final String DIALOG_TITLE = "PrintFlowLite Login";

    /**
     * The user id.
     */
    private String userId;

    /**
     * The password.
     */
    private String userPassword;

    /**
     *
     * @param userId
     *            The initial user id.
     */
    private UserPasswordDialog(final String userId) {
        this.userId = userId;
    }

    /**
     * Shows the {@link UserPasswordDialog}.
     *
     * @param userId
     *            The initial user id.
     * @return The the {@link UserPasswordDialog}.
     */
    public static UserPasswordDialog show(final String userId) {
        final UserPasswordDialog dialog = new UserPasswordDialog(userId);
        dialog.getIdAndPassword();
        return dialog;
    }

    /**
     *
     * @return {@code true} if the dialog was cancelled.
     */
    public boolean isCancelled() {
        return this.userId == null;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Gets the User ID and password using a modal dialog.
     */
    private void getIdAndPassword() {

        final String initialUserId = this.userId;

        final JPanel dialogPanel = new JPanel(false);

        dialogPanel.setLayout(new GridLayout(2, 2, 3, 3));

        final JLabel userNameLabel = new JLabel("User");
        @SuppressWarnings("serial")
        final JTextField userNameField = new JTextField(initialUserId) {
            @Override
            public void addNotify() {
                super.addNotify();
                if (initialUserId == null) {
                    requestFocus();
                }
            }
        };

        final JLabel passwordLabel = new JLabel("Password");
        @SuppressWarnings("serial")
        final JTextField passwordField = new JPasswordField() {
            @Override
            public void addNotify() {
                super.addNotify();
                if (initialUserId != null) {
                    requestFocus();
                }
            }
        };

        dialogPanel.add(userNameLabel);
        dialogPanel.add(userNameField);
        dialogPanel.add(passwordLabel);
        dialogPanel.add(passwordField);

        if (JOptionPane.showOptionDialog(null, dialogPanel, DIALOG_TITLE,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                DIALOG_BUTTON_NAMES, null) == 0) {

            userId = userNameField.getText();
            userPassword = passwordField.getText();

        } else {

            userId = null;
            userPassword = null;
        }

    }

}
