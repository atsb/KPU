/***
 *     KPU - KPF Patching Utility
 *     Copyright (C) 2025 atsb
 * <p>
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * <p>
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * <p>
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses.
 */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

public final class KPUGui {
    private JTextField tfKpf;
    private JTextField tfMod;
    private JButton btnPatch;

    private void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ignored) {}

        JFrame f = new JFrame("KPU - KPF Patching Utility");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(520, 160);
        f.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        // KPF
        c.gridx = 0; c.gridy = 0; c.weightx = 0; panel.add(new JLabel(
                "KPF:"), c);
        tfKpf = new JTextField(); tfKpf.setEditable(false);
        c.gridx = 1; c.gridy = 0; c.weightx = 1; panel.add(tfKpf, c);
        JButton bKpf = new JButton("Browse");
        bKpf.addActionListener(this::onBrowseKpf);
        c.gridx = 2; c.gridy = 0; c.weightx = 0; panel.add(bKpf, c);

        // MOD FOLDER
        c.gridx = 0; c.gridy = 1; c.weightx = 0; panel.add(new JLabel(
                "Mod folder:"), c);
        tfMod = new JTextField(); tfMod.setEditable(false);
        c.gridx = 1; c.gridy = 1; c.weightx = 1; panel.add(tfMod, c);
        JButton bMod = new JButton("Browse");
        bMod.addActionListener(this::onBrowseMod);
        c.gridx = 2; c.gridy = 1; c.weightx = 0; panel.add(bMod, c);

        // PATCH THAT SUCKER
        btnPatch = new JButton("Patch");
        btnPatch.addActionListener(this::onPatch);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        rightBar.add(btnPatch);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(rightBar, c);

        f.setContentPane(panel);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private void onBrowseKpf(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select KPF");
        fc.setFileFilter(new FileNameExtensionFilter(
                "KPF files", "kpf"));
        int r = fc.showOpenDialog(null);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            tfKpf.setText(f.getAbsolutePath());
        }
    }

    private void onBrowseMod(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Mod Folder");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fc.showOpenDialog(null);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            tfMod.setText(f.getAbsolutePath());
        }
    }

    private void onPatch(ActionEvent e) {
        String kpf = tfKpf.getText().trim();
        String mod = tfMod.getText().trim();
        if (kpf.isEmpty() || mod.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Please choose both the KPF and the Mod folder.",
                    "Missing paths", JOptionPane.WARNING_MESSAGE);
            return;
        }
        btnPatch.setEnabled(false);
        try {
            KPU.Options opt = new KPU.Options();
            KPU.patch(Path.of(kpf), Path.of(mod), opt);
            JOptionPane.showMessageDialog(null,
                    "Patched successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnPatch.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KPUGui().run());
    }
}
