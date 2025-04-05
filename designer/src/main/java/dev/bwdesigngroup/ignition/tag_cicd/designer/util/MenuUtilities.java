package dev.bwdesigngroup.ignition.tag_cicd.designer.util;

import javax.swing.*;
import java.awt.event.ActionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.inductiveautomation.ignition.client.icons.VectorIcons;

/**
 * Utilities for working with the Designer menu system.
 */
public class MenuUtilities {
    private static final Logger logger = LoggerFactory.getLogger(MenuUtilities.class.getName());

    /**
     * Adds a menu item to the specified top-level menu.
     * 
     * @param frame          The designer frame containing the menu bar
     * @param menuName       The name of the top-level menu to add to (e.g.,
     *                       "Tools")
     * @param itemName       The name of the menu item to add
     * @param iconName       The name of the VectorIcon to use for the menu item
     * @param actionListener The action to perform when the menu item is clicked
     * @param addToBeginning Whether to add the item at the beginning of the menu
     * @return True if the menu item was added successfully, false otherwise
     */
    public static boolean addMenuItemToTopLevelMenu(
            JFrame frame,
            String menuName,
            String itemName,
            String iconName,
            ActionListener actionListener,
            boolean addToBeginning) {

        try {
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar == null) {
                logger.warn("MenuBar not found");
                return false;
            }

            // Find the target menu
            JMenu targetMenu = null;
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && menuName.equals(menu.getText())) {
                    targetMenu = menu;
                    break;
                }
            }

            if (targetMenu == null) {
                logger.warn("Menu '{}' not found", menuName);
                return false;
            }

            // Create the menu item
            JMenuItem menuItem = new JMenuItem(itemName);
            if (iconName != null && !iconName.isEmpty()) {
                menuItem.setIcon(VectorIcons.getInteractive(iconName));
            }
            menuItem.addActionListener(actionListener);

            // Add the menu item at the specified position
            if (addToBeginning) {
                targetMenu.insert(menuItem, 0);
            } else {
                targetMenu.add(menuItem);
            }

            logger.info("Added menu item '{}' to '{}' menu", itemName, menuName);
            return true;
        } catch (Exception e) {
            logger.error("Error adding menu item '{}' to menu '{}'", itemName, menuName, e);
            return false;
        }
    }
}