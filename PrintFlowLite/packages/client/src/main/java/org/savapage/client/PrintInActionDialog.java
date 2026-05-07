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

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintInActionDialog extends JDialog
        implements ActionListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrintInActionDialog.class);

    /**
     * Pro forma serial id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * When {@code true} the application is shutdown after the dialog is
     * disposed.
     */
    private static boolean oneTimeTest = false;

    /**
     * The full-screen background of the {@link #printInDlg}.
     */
    private static JFrame ownerFrame;

    /**
     * The modal dialog on top of the {@link #ownerFrame}.
     */
    private static PrintInActionDialog printInDlg;

    /**
     * Border inset in pixels.
     */
    private static final int INSET = 20;

    /**
     * .
     */
    private static final String DIALOG_TITLE = "PrintFlowLite";

    /**
     * Opacity of the {@link #ownerFrame}.
     */
    private static final float OPACITY_LEVEL = 0.55f;

    /**
     * The top dialog with the action buttons.
     *
     * @param owner
     *            The owner {@link JFrame}.
     * @param message
     *            The message to display.
     * @param webAppUri
     *            The URI to open.
     * @param openButtonText
     *            The button text for opening the URL.
     */
    private PrintInActionDialog(final JFrame owner, final String message,
            final URI webAppUri, final String openButtonText) {

        super(owner, true);

        final JPanel messagePanel = new JPanel();
        messagePanel.setBorder(new EmptyBorder(INSET, INSET, INSET, INSET));
        getContentPane().add(messagePanel);
        messagePanel.add(new JLabel(message));

        /*
         * Buttons.
         */
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(0, INSET, INSET, INSET));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Button: Open Web App -> Print preview.
        final JButton buttonOpenBrowser = new JButton(openButtonText);
        buttonPanel.add(buttonOpenBrowser);
        buttonOpenBrowser.setToolTipText("Open the PrintFlowLite Web App...");
        buttonOpenBrowser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                openUserWebApp(webAppUri);
                onClose();
            }
        });
        registerEnter(buttonOpenBrowser);

        // Button: Close.
        final JButton buttonClose = new JButton("Close");
        buttonPanel.add(buttonClose);
        buttonClose.addActionListener(this);
        registerEnter(buttonClose);

        // no title bar
        setUndecorated(true);

        // not resizable
        setResizable(false);

        owner.getRootPane().setDefaultButton(buttonOpenBrowser);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // FIRST pack then set location (center on desktop).
        pack();

        // Center on desktop AFTER pack().
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        onClose();
    }

    /**
     * Registers ENTER keyboard action at button.
     *
     * @param button
     *            The {@link JButton}.
     */
    private static void registerEnter(final JButton button) {

        // button.setMargin(new Insets(5, 5, 5, 5));

        button.registerKeyboardAction(
                button.getActionForKeyStroke(
                        KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED);

        button.registerKeyboardAction(
                button.getActionForKeyStroke(
                        KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true),
                JComponent.WHEN_FOCUSED);
    }

    /**
     * Opens the browser with the Web App.
     *
     * @param webAppUri
     *            The URL.
     */
    private static void openUserWebApp(final URI webAppUri) {

        if (webAppUri != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(webAppUri);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * @return {@code true} when the GraphicsDevice supports translucency.
     */
    private static boolean isTranslucencySupported() {
        final GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.isWindowTranslucencySupported(TRANSLUCENT);
    }

    /**
     * Close dialog event.
     */
    private static synchronized void onClose() {

        LOGGER.info("closing dialog...");

        ownerFrame.setVisible(false);
        printInDlg.setVisible(false);

        printInDlg.dispose();
        ownerFrame.dispose();

        ownerFrame = null;
        printInDlg = null;

        LOGGER.info("dialog closed.");

        if (oneTimeTest) {
            System.exit(0);
        }
    }

    /**
     * Shows the message in dialog.
     *
     * @param message
     *            The message.
     * @param webAppUri
     *            The URI to open.
     * @param openButtonText
     *            The button text for opening the URL.
     * @return {@code true} when new dialog is shown, {@code false} when a
     *         dialog is already shown.
     */
    public static synchronized boolean showMessage(final String message,
            final URI webAppUri, final String openButtonText) {

        if (printInDlg != null) {
            LOGGER.info("dialog already open.");
            return false;
        }

        ownerFrame = new JFrame();
        ownerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Full effective screen size
        ownerFrame.setMinimumSize(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds().getSize());

        ownerFrame.setUndecorated(true); // no title bar
        ownerFrame.setResizable(false); // not resizible

        if (isTranslucencySupported()) {
            ownerFrame.setOpacity(OPACITY_LEVEL);
        }

        printInDlg = new PrintInActionDialog(ownerFrame, message, webAppUri,
                openButtonText);

        // Create the GUI on the event-dispatching thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("opening dialog...");
                ownerFrame.setVisible(true);
                printInDlg.setVisible(true);
            }
        });

        return true;
    }

    /**
     *
     * @param parms
     *            The CLI parms.
     */
    public static void main(final String[] parms) {

        oneTimeTest = true;

        BasicConfigurator.configure();

        PrintInActionDialog.showMessage("A Test Message.", null,
                "Open in browser");
    }

}