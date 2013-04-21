/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pathvisio.plugins;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PppExplore extends JFrame {
    private static void constructGUI () {
        JFrame.setDefaultLookAndFeelDecorated (true);
        JFrame frame = new JFrame ("BorderLayout Test");
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setLayout (new BorderLayout ());
        JTextField textField = new JTextField ("<your name>");
        frame.add (textField, BorderLayout.WEST);
        JButton button =
                new JButton ("<html>R<b>e</b>gister</html>");
        frame.add (button, BorderLayout.EAST);


        frame.pack ();
        frame.setVisible (true);

    }

    public static void main (String [] args) {
        SwingUtilities.invokeLater (new Runnable () {
            public void run () {
                constructGUI ();
            }

        });
    }
}





