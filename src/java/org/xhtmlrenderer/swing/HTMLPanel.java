/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */

package org.xhtmlrenderer.swing;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.render.*;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.layout.*;
import org.xhtmlrenderer.css.*;
import org.xhtmlrenderer.DefaultCSSMarker;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.util.logging.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.joshy.u;
import org.joshy.x;
import java.net.URL;
import java.io.File;
import org.apache.xpath.XPathAPI;
import org.xhtmlrenderer.forms.*;
import org.xhtmlrenderer.css.bridge.XRStyleReference;
import org.xhtmlrenderer.css.XRStyleSheet;


public class HTMLPanel extends JPanel implements ComponentListener
{
   public static Logger logger = Logger.getLogger("app.browser");

   //private int html_height = -1;
   //private int max_width = -1;

   public Document doc;
   public Context c;
   public Box body_box = null;
   private JScrollPane enclosingScrollPane;
   BodyLayout layout;
   private Dimension intrinsic_size;

   public Context getContext()
   {
      return c;
   }

   public HTMLPanel()
   {
      c = new Context();
      layout = new BodyLayout();
      setLayout(new AbsoluteLayoutManager());
      documentListeners = new HashMap();
   }

   Map documentListeners;

   public void addDocumentListener(DocumentListener listener)
   {
      this.documentListeners.put(listener, listener);
   }

   protected void fireDocumentLoaded()
   {
      Iterator it = this.documentListeners.keySet().iterator();
      while (it.hasNext())
      {
         DocumentListener list = (DocumentListener) it.next();
         list.documentLoaded();
      }
   }

   public String getDocumentTitle()
   {
      String title = "";
      try
      {
         Element root = this.doc.getDocumentElement();
         Node node =
         (Node) XPathAPI.selectSingleNode(root, "//head/title/text()");
         if (node == null)
         {
            System.err.println("Apparently no title element for this document.");
            title = "TITLE UNKNOWN";
         }
         else
         {
            title = node.getNodeValue();
         }
      }
      catch (Exception ex)
      {
         System.err.println("Error retrieving document title. " + ex.getMessage());
         title = "";
      }
      return title;
   }

   public void setDocumentRelative(String filename) throws Exception
   {
      if (c != null && (!filename.startsWith("http")))
      {
         URL base = new URL(c.getBaseURL(), filename);
         u.p("loading url " + base);
         Document dom = x.loadDocument(base);
         //URL base = new File(filename).toURL();
         setDocument(dom, base);
         return;
      }
      setDocument(x.loadDocument(filename), new File(filename).toURL());
   }


   protected ErrorHandler error_handler;

   public void setErrorHandler(ErrorHandler error_handler)
   {
      this.error_handler = error_handler;
   }


   private Document loadDocument(final URL url) throws Exception
   {
      DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = fact.newDocumentBuilder();
      builder.setErrorHandler(error_handler);
      return builder.parse(url.openStream());
   }


   public void setDocument(String filename) throws Exception
   {
      URL url = new File(filename).toURL();
      setDocument(loadDocument(url), url);
   }


   public void setDocument(URL url) throws Exception
   {
      setDocument(loadDocument(url), url);
   }


   public void setDocument(Document doc)
   {
      setDocument(doc, null);
   }


   public void resetScrollPosition()
   {
      if (this.enclosingScrollPane != null)
      {
         this.enclosingScrollPane.getVerticalScrollBar().setValue(0);
      }
   }

   public void setDocument(Document doc, URL url)
   {
      resetScrollPosition();
      this.doc = doc;

      Element html = (Element) doc.getDocumentElement();

      if (1 == 0)
      {
         c.css = new CSSBank();
      }
      else
      {
         // NOTE: currently context is externalized from StyleReference even
         // though it original design they had an ownership relationship (PWW 14/08/04)
         c.css = new XRStyleReference(c);
      }
      System.out.println("Using CSS implementation from: " + c.css.getClass().getName());

      c.setBaseURL(url);

      try
      {
         //c.css.parse(new FileReader("second.css"));
         Object marker = new DefaultCSSMarker();
         //u.p("getting: " + marker.getClass().getResource("default.css"));

         String defaultStyleSheetLocation = "/resources/css/default.css";
         if (marker.getClass().getResourceAsStream(defaultStyleSheetLocation) != null)
         {
            URL stream = marker.getClass().getResource(defaultStyleSheetLocation);
            //u.p("loading the url: " + stream);
            String str = u.inputstream_to_string(stream.openStream());
            //u.p("loaded: " + str);
            c.css.parse(new StringReader(str),
                        XRStyleSheet.USER_AGENT);//BufferInputStream(str));
         }
         else
         {
            u.p("can't load default css");
         }

         c.css.parseDeclaredStylesheets(html);
         c.css.parseLinkedStyles(html);
         c.css.parseInlineStyles(html);
      }
      catch (Exception ex)
      {
         u.p("error parsing CSS: " + ex);
         u.p(ex);
      }

      this.body_box = null;
      calcLayout();
      repaint();
   }


    /**
     * Overrides the default implementation to test for and configure any {@link JScrollPane}
     * parent.
     */
    public void addNotify() {
        super.addNotify();
        Container p = getParent();
        if (p instanceof JViewport) {
            Container vp = p.getParent();
            if (vp instanceof JScrollPane) {
                setEnclosingScrollPane((JScrollPane) vp);
            }
        }
    }

    /**
     * Overrides the default implementation unconfigure any {@link JScrollPane}
     * parent.
     */
    public void removeNotify() {
        super.removeNotify();
        setEnclosingScrollPane(null);
    }

    /**
     * The method is invoked by {@link #addNotify} and {@link #removeNotify} to ensure that
     * any enclosing {@link JScrollPane} works correctly with this panel.  This method can be
     * safely invoked with a <tt>null</tt> scrollPane.
     * @param scrollPane the enclosing {@link JScrollPane} or <tt>null</tt> if the panel is no longer
     * enclosed in a {@link JScrollPane}.
     */
    private void setEnclosingScrollPane(JScrollPane scrollPane) {
        // if a scrollpane is already installed we remove it.
        if (enclosingScrollPane != null)
            enclosingScrollPane.removeComponentListener(this);

        enclosingScrollPane = scrollPane;

        if (enclosingScrollPane != null)
            enclosingScrollPane.addComponentListener(this);
    }


   public void setAntiAliased(boolean anti_aliased)
   {
      this.anti_aliased = anti_aliased;
   }

   private boolean anti_aliased = true;


   public void paintComponent(Graphics g)
   {
      //g.setColor(Color.blue);
      //g.drawLine(0,0,50,50);
      //u.p("paint() size = " + this.getSize());
      //u.p("viewport size = " + this.viewport.getSize());
      //u.p("w/o scroll = " + this.viewport.getViewportBorderBounds());
      if (anti_aliased)
      {
         ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      doPaint(g);
   }

   //public static long timestamp;

   public void doPaint(Graphics g)
   {
      //u.p("paint");

      //long start_time = new java.util.Date().getTime();

      if (body_box == null)
      {
         calcLayout(g);
      }

      if (doc == null)
      {
         return;
      }

      newContext(g);
      //u.p("paint");
      layout.paint(c, body_box);
      //long end_time = new java.util.Date().getTime();
      //u.p("dist = " + (end_time-start_time));
   }


   public void calcLayout()
   {
      calcLayout(this.getGraphics());
      this.setOpaque(false);
   }


   private void newContext(Graphics g)
   {
      Point origin = new Point(0, 0);
      Point last = new Point(0, 0);
      c.canvas = this;
      c.graphics = g;
      // set up the dimensions of the html canvas
      //Rectangle dimensions = new Rectangle(this.getWidth(),this.getHeight());//layout.bwidth, layout.bheight);
      //c.canvas_graphics = g.create();
      //c.setExtents(new Rectangle(0,0,this.getWidth(),this.getHeight()));
      //u.p("viewport size = " + viewport.getSize());
      if (enclosingScrollPane != null)
      {
         c.setExtents(new Rectangle(enclosingScrollPane.getViewportBorderBounds()));
      }
      else
      {
         c.setExtents(new Rectangle(200, 200));
      }

      //c.setExtents(new Rectangle(0,0,viewport.getWidth(),viewport.getHeight()));
      c.viewport = this.enclosingScrollPane;
      c.cursor = last;
      c.setMaxWidth(0);
   }


   public void calcLayout(Graphics g)
   {
      //u.p("calcLayout()");
      this.removeAll();
      //u.p("this = ");
      //u.dump_stack();
      if (g == null)
      {
         //u.p("bad first graphics");
         return;
      }
      //u.p("layout");
      //u.p("size = " + this.getSize());
      //u.dump_stack();
      if (doc == null)
      {
         return;
      }

      Element html = (Element) doc.getDocumentElement();
      Element body = x.child(html, "body");
      body = html;

      newContext(g);
      // set up CSS
      // the last added is used first
      // start painting
      c.setMaxWidth(0);
      long start_time = new java.util.Date().getTime();
      //u.p("starting count");
      body_box = layout.layout(c, body);
      long end_time = new java.util.Date().getTime();
      //u.p("ending count = " + (end_time-start_time) + " msec");

      if (enclosingScrollPane != null)
      {
         if (this.body_box != null)
         {
            // CLN
            //u.p("bcolor = " + body_box.background_color);
            //u.p("body box = " + body_box.hashCode());
            this.enclosingScrollPane.getViewport().setBackground(body_box.background_color);
         }
      }

      //u.p("calced height = " + layout.contents_height);//this.html_height);
      //u.p("max width = " + c.getMaxWidth());
      intrinsic_size = new Dimension(c.getMaxWidth(), layout.contents_height);
      //u.p("intrinsic size = " + intrinsic_size);
      //u.p("real size = " + this.getSize());
      if (!intrinsic_size.equals(this.getSize()))
      {
         //u.dump_stack();
         this.setPreferredSize(intrinsic_size);
         //u.p("setting preferred to : " + this.getPreferredSize());
         //this.setSize(intrinsic_size);
         this.revalidate();
         //this.repaint();
         //this.setPreferredSize(intrinsic_size);
      }

      //this.html_height = layout.contents_height;
      /*if(c.getMaxWidth() > this.getSize().getWidth()) {
          this.max_width = c.getMaxWidth();
      }
      //u.p("html height = " + this.html_height);
      if(c.getMaxWidth() > this.getSize().getWidth()) {
          this.setPreferredSize(new Dimension((int)c.getMaxWidth(),this.html_height));
          this.max_width = c.getMaxWidth();
          this.setMinimumSize(this.getPreferredSize());
      } else {
          this.setPreferredSize(new Dimension((int)this.getSize().getWidth(),this.html_height));
          this.max_width = (int)this.getSize().getWidth();
      }
      */

      /*
      u.p("size = " + this.getSize());
      u.p("pref size = " + this.getPreferredSize());
      if(!this.getSize().equals(this.getPreferredSize())) {
          u.p("need a repaint");
          u.p("size = " + this.getSize());
          u.p("pref size = " + this.getPreferredSize());
          super.setSize(this.getPreferredSize());
          repaint();
      }
      */

      //c.getGraphics().setColor(Color.blue);
      //c.getGraphics().drawLine(0,0,50,50);
      this.fireDocumentLoaded();
   }


   public void setSize(Dimension d)
   {
      //u.p("set size called");
      super.setSize(d);
      this.calcLayout();
   }



   /* === scrollable implementation === */
   /*
   public Dimension getPreferredScrollableViewportSize() {
       u.p("get pref scrll view called");
       //u.dump_stack();

       //u.p("size of viewport = " + viewport.getSize());
       //u.p("size of intrinsic = " + intrinsic_size);
       u.dump_stack();
       if(intrinsic_size == null) {
           return new Dimension(400,400);
       }

       if(intrinsic_size.getWidth() > viewport.getWidth()) {
           //u.p("intrinsic  = " + this.intrinsic_size);
           return new Dimension((int)intrinsic_size.getWidth(),400);
       }

       return null;
   }

   public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
       return 20;
   }

   public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
       return 100;
   }

   public boolean getScrollableTracksViewportWidth() {
       return false;
   }

   public boolean getScrollableTracksViewportHeight() {
       return false;
   }

   */

   public Box findBox(int x, int y)
   {
      return findBox(this.body_box, x, y);
   }

   public Box findBox(Box box, int x, int y) {

      if (box instanceof LineBox || box instanceof InlineBox) {
         //u.p("findBox(" + box + " at ("+x+","+y+")");
      }

      Iterator it = box.getChildIterator();
      while (it.hasNext()) {
         Box bx = (Box) it.next();
         int tx = x;
         int ty = y;
         tx -= bx.x;
         tx -= bx.totalLeftPadding();
         ty -= bx.y;
         ty -= bx.totalTopPadding();

         // test the contents
         Box retbox = null;
         retbox = findBox(bx, tx, ty);
         if (retbox != null) {
            return retbox;
         }

         // test the box itself
         //u.p("bx test = " + bx + " " + x +","+y);
         int tty = y;
         if (bx instanceof InlineBox) {
            InlineBox ibx = (InlineBox) bx;
            LineBox lbx = (LineBox) box;
            //u.p("inline = " + ibx);
            //u.p("inline y = " + ibx.y);
            //u.p("inline height = " + ibx.height);
            //u.p("line = " + lbx);
            int off = lbx.baseline + ibx.y - ibx.height;
            //u.p("off = " + off);
            tty -= off;
         }

         if (bx.contains(x - bx.x, tty - bx.y)) {
            return bx;
         }
      }

      return null;
   }


   public int findBoxX(int x, int y)
   {
      return findBoxX(this.body_box, x, y);
   }


   public int findBoxX(Box box, int x, int y)
   {
      //u.p("findBox(" + box + " at ("+x+","+y+")");
      Iterator it = box.getChildIterator();

      while (it.hasNext())
      {
         Box bx = (Box) it.next();
         int tx = x;
         int ty = y;
         tx -= bx.x;
         tx -= bx.totalLeftPadding();
         ty -= bx.y;
         ty -= bx.totalTopPadding();

         // test the contents
         int retbox = findBoxX(bx, tx, ty);
         if (retbox != -1)
         {
            return retbox;
         }

         int tty = y;
         if (bx instanceof InlineBox)
         {
            InlineBox ibx = (InlineBox) bx;
            LineBox lbx = (LineBox) box;
            //u.p("inline = " + ibx);
            //u.p("inline y = " + ibx.y);
            //u.p("inline height = " + ibx.height);
            //u.p("line = " + lbx);
            int off = lbx.baseline + ibx.y - ibx.height;
            //u.p("off = " + off);
            tty -= off;
         }

         // test the box itself
         //u.p("bx test = " + bx + " " + x +","+y);
         if (bx.contains(x - bx.x, tty - bx.y))
         {
            return x - bx.x;
         }
      }

      return -1;
   }

   public void componentHidden(ComponentEvent e)
   {
   }

   public void componentMoved(ComponentEvent e)
   {
   }

   public void componentResized(ComponentEvent e)
   {
      calcLayout();
   }

   public void componentShown(ComponentEvent e)
   {
   }


   public void printTree()
   {
      printTree(this.body_box, "");
   }

   private void printTree(Box box, String tab)
   {
      u.p(tab + "Box = " + box);
      Iterator it = box.getChildIterator();
      while (it.hasNext())
      {
         Box bx = (Box) it.next();
         printTree(bx, tab + " ");
      }

      if (box instanceof InlineBox)
      {
         InlineBox ib = (InlineBox) box;
         if (ib.sub_block != null)
         {
            printTree(ib.sub_block, tab + " ");
         }
      }
   }
}



