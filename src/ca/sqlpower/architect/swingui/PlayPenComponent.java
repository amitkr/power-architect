/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.architect.swingui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.architect.swingui.event.SelectionListener;

/**
 * PlayPenComponent is the base class for a component that can live in the playpen's
 * content pane.
 */
public abstract class PlayPenComponent implements Selectable {

    private static final Logger logger = Logger.getLogger(PlayPenComponent.class);
    
    private PlayPenContentPane parent;
    private Rectangle bounds = new Rectangle();
    protected Color backgroundColor;
    protected Color foregroundColor;
    private Insets insets = new Insets(0,0,0,0);
    private String toolTipText;
    private boolean opaque;
    
    private PlayPenComponentUI ui;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * A selected component is one that the user has clicked on. It will appear
     * more prominently than non-selected ContainerPane, and its status as
     * selected makes it the target of actions that are invoked on the playpen.
     */
    protected boolean selected;

    protected boolean componentPreviouslySelected;

    /**
     * Copy constructor. Makes deep copies of all PlayPenComponent state.
     * Subclasses that implement a copy constructor should chain to this
     * constructor in their own copy constructors.
     * <p>
     * The parent reference of the new copy will be null. Copy constructors in
     * subclasses should also leave the parent pointer null--it is up to the
     * code initiating the copy to add the newly-copied component to some parent
     * (if you want it to belong to some parent).
     * 
     * @param copyMe the playpen component this new component should be a copy of
     * @param parent the parent content pane of this new copy
     */
    protected PlayPenComponent(PlayPenComponent copyMe, PlayPenContentPane parent) {
        backgroundColor = copyMe.backgroundColor;
        if (copyMe.bounds != null) {
            bounds = new Rectangle(copyMe.bounds);
        }
        componentPreviouslySelected = copyMe.componentPreviouslySelected;
        foregroundColor = copyMe.foregroundColor;
        if (copyMe.insets != null) {
            insets = new Insets(
                    copyMe.insets.top, copyMe.insets.left,
                    copyMe.insets.bottom, copyMe.insets.right);
        }
        opaque = copyMe.opaque;
        this.parent = parent;
        // pcs should not be copied
        selected = copyMe.selected;
        // selectionListeners should not be copied
        toolTipText = copyMe.toolTipText;
        // ui should not be copied, but subclass should call updateUI()
    }
    
    protected PlayPenComponent(PlayPenContentPane parent) {
        this.parent = parent;
    }

    public PlayPen getPlayPen() {
        if (parent == null) return null;
        return parent.getOwner();
    }

    
    public PlayPenComponentUI getUI() {
        return ui;
    }

    public void setUI(PlayPenComponentUI ui) {
        this.ui = ui;
        revalidate();
    }

    /**
     * Shows the component's popup menu on the PlayPen that owns this component
     * because it doesn't work to show it on this component, which is not really
     * part of the swing hierarchy. Only executed if the component has a popup
     * menu, see {@link #getPopup()}.
     * 
     * @param p
     *            the point (relative to this component's top-left corner) to
     *            show it at.
     */
    public void showPopup(Point p) {
    	JPopupMenu menu = getPopup();
        if (menu != null) {
            final int xAdjust = 5;  // ensure menu doesn't appear directly under pointer
            p.translate(getX(), getY());
            getPlayPen().zoomPoint(p);
            menu.show(getPlayPen(), p.x + xAdjust, p.y);
        }
    }
    
    /**
     * Returns a component specific popup menu. Defaulted here to null
     * so components that have popup menus must override this class. 
     */
    public JPopupMenu getPopup() {
        return null;
    }
    
    /**
     * Translates this request into a call to
     * PlayPen.repaint(Rectangle).  That will eventually cause a call
     * to PlayPen.paint(). Painting a TablePane causes it to re-evaluate its
     * preferred size, which is what validation is really all about.
     */
    public void revalidate() {
        PlayPen pp = getPlayPen();
        if (pp == null) {
            logger.debug("getPlayPen() returned null.  Not generating repaint request."); //$NON-NLS-1$
            return;
        }
        Rectangle r = new Rectangle(bounds);
        PlayPenComponentUI ui = getUI();
        if (ui != null) {
            ui.revalidate();
            Dimension ps = ui.getPreferredSize();
            if (ps != null) setSize(ps);
        }
        pp.zoomRect(r);
        if (logger.isDebugEnabled()) logger.debug("Scheduling repaint at "+r); //$NON-NLS-1$
        pp.repaint(r);
    }

    /**
     * Updates the bounds of this component, then issues a repaint to the
     * PlayPen which covers the old bounds of this component. This will allow
     * newly-exposed sections of the PlayPen to draw themselves in case this
     * setBounds call is shrinking this component.  Also ensures the new bounds
     * do not remain left of or above the (0,0) point by normalizing the play pen.
     * 
     * <p>All methods that affect the bounds rectangle should do so by calling this
     * method.
     */
    protected void setBoundsImpl(int x, int y, int width, int height) { 
        Rectangle oldBounds = getBounds(); 
        PlayPen owner = getPlayPen();
        if (owner != null) {
            Rectangle r = getBounds();
            double zoom = owner.getZoom();
            owner.repaint((int) Math.floor((double) r.x * zoom),
                          (int) Math.floor((double) r.y * zoom),
                          (int) Math.ceil((double) r.width * zoom),
                          (int) Math.ceil((double) r.height * zoom));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Updating bounds on "+getName() //$NON-NLS-1$
                         +" to ["+x+","+y+","+width+","+height+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        Point oldPoint = new Point(bounds.x,bounds.y);
        bounds.setBounds(x,y,width,height);

        if (oldBounds.x != x || oldBounds.y != y) {
            firePropertyChange(new PropertyChangeEvent(this, "location", oldPoint, new Point(x,y)));
        }
        if (oldBounds.width != width || oldBounds.height != height) {
            firePropertyChange(new PropertyChangeEvent(this, "bounds", null, null));
        }

        repaint();
    }

    /**
     * See setBoundsImpl.
     */
    public void setBounds(int x, int y, int width, int height) {
        setBoundsImpl(x, y, width, height);
    }

    /**
     * Returns a copy of this component's bounding rectangle.
     */
    public Rectangle getBounds() {
        return getBounds(null);
    }

    /**
     * Sets the given rectangle to be identical to this component's bounding box.
     * 
     * @param r An existing rectangle.  If null, this method creates a new rectangle for you.
     * @return r if r was not null; a new rectangle otherwise.
     */
    public Rectangle getBounds(Rectangle r) {
        if (r == null) r = new Rectangle();
        r.setBounds(bounds);
        return r;
    }

    public Dimension getSize() {
        return new Dimension(bounds.width, bounds.height);
    }
    
    /**
     * The revalidate() call uses this to determine the component's
     * correct location.  This implementation just returns the current
     * location.  Override it if you need to be moved during validation.
     */
    public Point getPreferredLocation() {
        return getLocation();
    }

    public Point getLocation() {
        return getLocation(null);
    }
    
    /**
     * Copies this component's location into the given point object.
     * 
     * @param p A point that this method will modify.  If you pass in null, this method will
     * create a new point for you.
     * @return p if p was not null; a new point otherwise.
     */
    public Point getLocation(Point p) {
        if (p == null) p = new Point();
        p.x = bounds.x;
        p.y = bounds.y;
        return p;
    }
    
    /**
     * Updates the on-screen location of this component.  If you try to move this
     * component to a negative co-ordinate, it will automatically be normalized (along
     * with everything else in the playpen) to non-negative coordinates.
     */
    public void setLocation(Point point) {
        setBoundsImpl(point.x,point.y, getWidth(), getHeight());
    }

    /**
     * Updates the on-screen location of this component.  If you try to move this
     * component to a negative co-ordinate, it will automatically be normalized (along
     * with everything else in the playpen) to non-negative coordinates.
     */
    public void setLocation(int x, int y) {
        setBoundsImpl(x, y, getWidth(), getHeight());
    }
    
    public void setSize(Dimension size) {
        setBoundsImpl(getX(), getY(), size.width, size.height);
    }

    /**
     * Forwards to {@link #repaint(Rectangle)}.
     */
    public void repaint() {
        repaint(getBounds());
    }

    /**
     * Forwards to {@link #repaint(long,int,int,int,int)}.
     */
    public void repaint(Rectangle r) {
        repaint(0, r.x, r.y, r.width, r.height);
    }

    /**
     * Returns the user-visible name for this component--often the same as
     * getModel().getName(), but this depends entirely on the subclass's idea
     * of what in the model constitutes its name.
     */
    public abstract String getName();

    /**
     * Adds a property change listener to the existing list.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
    
    /**
     * Adds a property change listener for a specific property.
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        pcs.addPropertyChangeListener(propertyName, l);
    }
    
    /**
     * Removes a specific property change listener from the existing list.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
    
    /**
     * Notifies property change listeners of a property change event.
     */
    protected void firePropertyChange(String propName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propName, oldValue, newValue);
    }
    
    /**
     * @see PlayPenComponent.firePropertyChange()
     */
    public void firePropertyChange(PropertyChangeEvent e) {
        pcs.firePropertyChange(e);
    }
    
    public int getX() {
        return bounds.x;
    }
    
    public int getY() {
        return bounds.y;
    }
    
    public int getWidth() {
        return bounds.width;
    }
    
    public int getHeight() {
        return bounds.height;
    }
    
    public Insets getInsets() {
        return new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }

    public void setInsets(Insets insets) {
        this.insets = new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }

    /**
     * Tells the owning PlayPen to repaint the given region.  The
     * rectangle is manipulated (zoomed) into screen coordinates.
     */
    public void repaint(long tm,
                        int x,
                        int y,
                        int width,
                        int height) {
        PlayPen owner = getPlayPen();
        if (owner == null) return;
        double zoom = owner.getZoom();
        owner.repaint((int) Math.floor((double) x * zoom),
                      (int) Math.floor((double) y * zoom),
                      (int) Math.ceil((double) width * zoom),
                      (int) Math.ceil((double) height * zoom));
    }
    
    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
        revalidate();
    }

    public Color getBackgroundColor() {
        if (backgroundColor == null) {
            return getPlayPen().getBackground();
        }
        return backgroundColor;
    }
    
    public void setBackgroundColor(Color c) {
        Color oldColor = backgroundColor;
        backgroundColor = c;
        revalidate();
        firePropertyChange("backgroundColor", oldColor, backgroundColor);
    }

    public Color getForegroundColor() {
        if (foregroundColor == null) {
            return getPlayPen().getForeground();
        }
        return foregroundColor;
    }
    
    public void setForegroundColor(Color c) {
        Color oldColor = foregroundColor;
        foregroundColor = c;
        revalidate();
        firePropertyChange("foregroundColor", oldColor, foregroundColor);
    }
    
    public String getToolTipText() {
        return toolTipText;
    }

    public void setToolTipText(String toolTipText) {
        if ((toolTipText == null && this.toolTipText == null) 
                || (toolTipText != null && toolTipText.equals(this.toolTipText))) {
            return;
        }
        this.toolTipText = toolTipText;
        logger.debug("ToolTipText changed to "+toolTipText); //$NON-NLS-1$
    }

    public Font getFont() {
        return getPlayPen().getFont();
    }

    public FontMetrics getFontMetrics(Font f) {
        return getPlayPen().getFontMetrics(f);
    }
    
    public FontRenderContext getFontRenderContext() {
        return getPlayPen().getFontRenderContext();
    }
    
    public boolean contains(Point p) {
        boolean containsPoint = getUI().contains(p);
        logger.debug("" + this + " contains " + p + "? " + containsPoint);
        return containsPoint;
    }

    public void paint(Graphics2D g2) {
        getUI().paint(g2);
        if (logger.isDebugEnabled()) {
            Color oldColor = g2.getColor();
            g2.setColor(Color.ORANGE);
            g2.drawRect(0, 0, getWidth(), getHeight());
            g2.setColor(oldColor);
        }
    }

    public Dimension getPreferredSize() {
        return getUI().getPreferredSize();
    }

    public abstract Object getModel();

    public PlayPenContentPane getParent() {
        return parent;
    }
    
    /**
     * Performs the component specific actions for the given MouseEvent. 
     */
    public abstract void handleMouseEvent(MouseEvent evt);

    // --------------------- SELECTABLE SUPPORT ---------------------

    private final List<SelectionListener> selectionListeners = new LinkedList<SelectionListener>();

    public final void addSelectionListener(SelectionListener l) {
        selectionListeners.add(l);
    }

    public final void removeSelectionListener(SelectionListener l) {
        selectionListeners.remove(l);
    }

    protected final void fireSelectionEvent(SelectionEvent e) {
        if (logger.isDebugEnabled()) {
            logger.debug("Notifying "+selectionListeners.size() //$NON-NLS-1$
                         +" listeners of selection change"); //$NON-NLS-1$
        }
        Iterator<SelectionListener> it = selectionListeners.iterator();
        if (e.getType() == SelectionEvent.SELECTION_EVENT) {
            while (it.hasNext()) {
                it.next().itemSelected(e);
            }
        } else if (e.getType() == SelectionEvent.DESELECTION_EVENT) {
            while (it.hasNext()) {
                it.next().itemDeselected(e);
            }
        } else {
            throw new IllegalStateException("Unknown selection event type "+e.getType()); //$NON-NLS-1$
        }
    }

    /**
     * See {@link #selected}.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Tells this component it is selected or deselected. If isSelected is different
     * from the current selection state for this component, a SelectionEvent will be
     * fired to all selection listeners.
     * <p>
     * See {@link #selected}.
     * 
     * @param isSelected The new selection state for this component
     * @param multiSelectType One of the type codes from {@link SelectionEvent}.
     */
    public void setSelected(boolean isSelected, int multiSelectType) {
        if (selected != isSelected) {
            selected = isSelected;
            fireSelectionEvent(new SelectionEvent(this, selected ? SelectionEvent.SELECTION_EVENT : SelectionEvent.DESELECTION_EVENT, multiSelectType));
            repaint();
        }
    }
}
