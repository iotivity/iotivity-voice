/*
 *******************************************************************
 *
 * Copyright 2017 Intel Corporation.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

package org.iotivity.base.examples;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * LightPanel
 */
public class LightPanel extends JLabel implements Observer {

    private boolean powerOn;
    private int brightness;

    private BufferedImage imageOn;
    private BufferedImage imageOff;
    private BufferedImage composite;

    public LightPanel(boolean powerOn, int brightness) {

        this.powerOn = powerOn;
        this.brightness = brightness;

        try {
            imageOn = ImageIO.read(LightPanel.class.getResource("/res/light-on.png"));
            imageOff = ImageIO.read(LightPanel.class.getResource("/res/light-off.png"));

            composite = new BufferedImage(imageOff.getWidth(), imageOff.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics g = composite.getGraphics();
            g.drawImage(imageOff, 0, 0, null);

            float alpha = powerOn ? ((float) brightness / 100.0f) : 0.0f;
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            ((Graphics2D) g).setComposite(ac);
            g.drawImage(imageOn, 0, 0, null);

            ImageIcon imageIcon = new ImageIcon(composite);
            setIcon(imageIcon);

            setComponentPopupMenu(new SettingsPopupMenu(this));

        } catch (IOException e) {
            OcfLightDevice.msgError("Error creating light image: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(imageOff, 0, 0, null);

        float alpha = powerOn ? ((float) brightness / 100.0f) : 0.0f;
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        ((Graphics2D) g).setComposite(ac);
        g.drawImage(imageOn, 0, 0, null);
    }

    @Override
    public void update(Observable o, Object arg) {
        boolean repaintRequired = false;
        if (o instanceof Switch) {
            powerOn = ((Switch) o).getValue();
            SettingsPopupMenu popupMenu = (SettingsPopupMenu) getComponentPopupMenu();
            popupMenu.setPowerOn(powerOn);
            repaintRequired = true;

        } else if (o instanceof Brightness) {
            brightness = ((Brightness) o).getBrightness();
            SettingsPopupMenu popupMenu = (SettingsPopupMenu) getComponentPopupMenu();
            popupMenu.setBrightness(brightness);
            repaintRequired = true;

        } else if (o instanceof LightConfig) {
            String deviceName = ((LightConfig) o).getDeviceName();
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.setTitle(deviceName);

        } else if (o instanceof Light) {
            String deviceName = ((Light) o).getDeviceName();
            powerOn = ((Light) o).getPowerOn();
            brightness = ((Light) o).getBrightness();

            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.setTitle(deviceName);

            SettingsPopupMenu popupMenu = (SettingsPopupMenu) getComponentPopupMenu();
            popupMenu.setPowerOn(powerOn);
            popupMenu.setBrightness(brightness);
            repaintRequired = true;

        } else {
            // ignore
        }

        if (repaintRequired) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    repaint();
                }
            });
        }
    }

    // LightImageObservable implementation
    private Map<Integer, LightImageObserver> observers = new ConcurrentHashMap<Integer, LightImageObserver>();

    public void addObserver(LightImageObserver observer) {
        observers.put(observer.hashCode(), observer);
    }

    public void notifyObservers() {
        Thread observerNotifier = new Thread(new Runnable() {
            public void run() {
                for (LightImageObserver observer : observers.values()) {
                    observer.update(powerOn, brightness);
                }
            }
        });
        observerNotifier.setDaemon(true);
        observerNotifier.start();
    }

    private class SettingsPopupMenu extends JPopupMenu {
        private JCheckBoxMenuItem powerItem;
        private JMenuItem brightnessItem;
        private SpinnerNumberModel model;

        public SettingsPopupMenu(final LightPanel lightPanel) {
            powerItem = new JCheckBoxMenuItem("On");
            powerItem.setSelected(lightPanel.powerOn);
            powerItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    lightPanel.powerOn = powerItem.isSelected();
                    lightPanel.repaint();
                    lightPanel.notifyObservers();
                }
            });
            add(powerItem);

            brightnessItem = new JMenuItem();
            model = new SpinnerNumberModel(lightPanel.brightness, 0, 100, 1);
            JSpinner spinner = new JSpinner(model);
            brightnessItem.add(spinner);

            brightnessItem.setPreferredSize(powerItem.getPreferredSize());
            spinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    lightPanel.brightness = (int) model.getValue();
                    lightPanel.repaint();
                    lightPanel.notifyObservers();
                }
            });
            add(brightnessItem);
        }

        public void setPowerOn(boolean powerOn) {
            powerItem.setSelected(powerOn);
        }

        public void setBrightness(int brightness) {
            model.setValue(brightness);
        }
    }
}
