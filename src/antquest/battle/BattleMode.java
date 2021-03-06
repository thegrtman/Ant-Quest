/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package antquest.battle;

import antquest.menus.TextElement;
import antquest.menus.MenuBlock;
import antquest.menus.LiveMenu;
import antquest.*;
import antquest.battle.areas.*;
import antquest.menus.*;
import antquest.numerical.Gradient;
import antquest.numerical.NumberRange;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 *
 * @author Kevin
 */
public class BattleMode extends LiveMenu {

   //Even y-value means it is shifted to the right
   public static final int DEFAULT_WIDTH = 25;
   public static final int DEFAULT_HEIGHT = 25;
   public static final double PERIOD = 50;
   public static final int MENU_WIDTH = 100;
   public static final int MENU_HEIGHT = 80;
   public static final int HEX_SIZE = 16;
   public static final int MODE_RENDER = 0;
   public static final int MODE_CURSOR = 1;
   public static final int MODE_PLAYER = 2;
   public static final int MODE_COMPUTER = 3;
   public static final int MODE_AOE = 4;
   protected Gradient cursorGrad, offensiveGrad, defensiveGrad, neutralGrad;
   protected Color bgColor;
   protected final int mapWidth, mapHeight;
   protected int cx, cy, ocx, ocy;
   protected ArrayList<BattleActor> actors;
   protected BattleEntity current;
   protected NumberRange[] ranges;
   protected String[] rangeNames;
   protected int rangeSetter;
   protected Hex[][] battlemap;
   protected double cursorFrame;
   protected int currentMode;
   protected double brightness;
   protected Color[] cols;
   protected boolean moved, cursor, useScatter;
   protected double tb;
   protected BattleSkill skill;
   protected TextElement debug1, debug2, debug3;
   protected ListBlock skillList;
   protected MenuBlock debugBlock, otherBlock;
   protected BattleAnimEffect animation;

   public BattleMode() {
      this(AQEngine.randInt(60) - 30);
   }

   public BattleMode(double bright) {
      super(AQEngine.getCurrentMode(), null);
      debug1 = new TextElement(10, AQEngine.getHeight() - 100);
      debug2 = new TextElement(10, AQEngine.getHeight() - 80);
      debug3 = new TextElement(10, AQEngine.getHeight() - 60);
      brightness = bright;
      currentMode = MODE_CURSOR;
      battlemap = new Hex[DEFAULT_WIDTH][DEFAULT_HEIGHT];
      mapWidth = DEFAULT_WIDTH;
      mapHeight = DEFAULT_HEIGHT;
      System.out.println("Brightness: " + brightness);
      for (int i = 0; i < mapWidth; i++) {
         for (int j = 0; j < mapHeight; j++) {
            int terr = (int) (AQEngine.randDouble() * 31 - 15 + brightness);
            terr = terr < -5 ? Hex.TERRAIN_DARK : terr > 5 ? Hex.TERRAIN_BRIGHT : 0;
            battlemap[i][j] = new Hex(this, i, j, (AQEngine.randInt(10) - 5), terr);
            if (AQEngine.randDouble() < .2) {
               battlemap[i][j].setOccupant(new BattleActor(null, 0, false));
            }
         }
      }
      cx = mapWidth / 2;
      cy = mapHeight / 2;
      cursorFrame = 0;
      blocks.add(debugBlock = new MenuBlock(this, 5, AQEngine.getHeight() - 105, AQEngine.getWidth() - 10, 100));
      debugBlock.add(debug1);
      debugBlock.add(debug2);
      debugBlock.add(debug3);
      blocks.add(skillList = new ListBlock(this, 5, 5, 100, AQEngine.getHeight() - 115, TextElement.DEFAULT_FONT));
      blocks.add(otherBlock = new MenuBlock(this, AQEngine.getWidth() - 105, 5, 100, AQEngine.getHeight() - 115));
      cols = new Color[64];
      for (int i = 0; i < 64; i++) {
         cols[i] = hexColor(i);
      }
      skill = null;
      ranges = null;
      rangeNames = null;
      rangeSetter = 0;
      moved = true;
      cursor = true;
      double pow = .25;
      tb = Math.signum(brightness) * Math.pow(Math.abs(brightness), pow) * Math.pow(30, 1 - pow);
      System.out.println("HERE: " + tb);
      int r = (int) (90 + 2.5 * tb);
      int g = (int) (90 + 2.5 * tb);
      int b = (int) (128 + 3 * tb);
      bgColor = new Color(r, g, b);
      
      cursorGrad = new Gradient(180, 180, 0, 255, 255, 255);
      offensiveGrad = new Gradient(192, 0, 0, 64, 0, 0);
      defensiveGrad = new Gradient(100, 100, 255, 100, 255, 255);
      neutralGrad = new Gradient(255, 192, 0, 0, 0, 255);
      setDebugText();
      setSkillListText(entityAtCursor());
   }

   final void setDebugText() {
      debug1.setText(cx + " / " + cy + " / " + battlemap[cx][cy].zpos);
      debug2.setText("Brightness: " + brightness + " / " + tb);
      if(skill != null){
         if(rangeSetter < 0){
            debug3.setText("X of origin: " + cx + " / Y of origin: " + cy);
         }else{
            String text = rangeNames[rangeSetter] + ": " + ranges[rangeSetter].getCurrent();
            if(rangeSetter + 1 < ranges.length){
               text += " / " + rangeNames[rangeSetter + 1] + ": " + ranges[rangeSetter + 1].getCurrent();
            }
            debug3.setText(text);
         }
      }else{
         debug3.setText("");
      }
   }
   
   @Override
   public void press(KeyEvent e) {
      int trans = InputHelper.transform(e);

      if (e.getKeyCode() == KeyEvent.VK_F3) {
         System.out.println("PRINTING MAP:");
         for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
               System.out.println("  [" + x + "][" + y + "]: " + battlemap[x][y].zpos);
            }
         }
         System.out.println("CURSOR ON: [" + cx + "][" + cy + "]");
      }

      final int TYPES = 6;
      int delta = 24;

      //<editor-fold defaultstate="collapsed" desc="MODE_CURSOR">
      if (currentMode == MODE_CURSOR) {
         
         Point p = buttonMovement(trans);
         moveCursor(p);
         
         clearSkillList();
         
         BattleEntity selectedEntity = entityAtCursor();
         if ((trans & InputHelper.CONFIRM) != 0) {
            if (selectedEntity != null) {
               setSkillListButton(selectedEntity);
               current = selectedEntity;
               currentMode = MODE_PLAYER;
               lr = false;
               ud = true;
               ocx = cx;
               ocy = cy;
               CONFIRM.forcePlay(true, true);
            } else {
               ERROR.tryPlay(true, true);
            }
         }else{
            setSkillListText(selectedEntity);
         }
      }
      //</editor-fold>
      
      //<editor-fold defaultstate="collapsed" desc="MODE_PLAYER">
      else if (currentMode == MODE_PLAYER) {
         if((trans & InputHelper.CANCEL) != 0){
            clearSkillList();
            setSkillListText(entityAtCursor());
            selected = null;
            currentMode = MODE_CURSOR;
            CANCEL.forcePlay(true, true);
         }else{
            super.press(e);
         }
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE AOE">
      else if (currentMode == MODE_AOE){
         Point p = buttonMovement(trans);
         moveRanges(p, rangeSetter);
         if((trans & InputHelper.CONFIRM) != 0){
            if(rangeSetter + 2 < ranges.length){
               rangeSetter += 2;
            }else{
               animation = skill.apply(current, (BattleMode)self, cx, cy, getAOEVals());
               rangeSetter = 0;
               skill = null;
               ranges = null;
               rangeNames = null;
               currentMode = MODE_RENDER;
               cx = ocx;
               cy = ocy;
               selected = null;
            }
            CONFIRM.forcePlay(true, true);
         }
         
         if((trans & InputHelper.CANCEL) != 0){
            if(rangeSetter < 0 || (rangeSetter-2 < 0 && !skill.canMoveOrigin())){
               rangeSetter = 0;
               skill = null;
               ranges = null;
               rangeNames = null;
               currentMode = MODE_PLAYER;
            }else{
               rangeSetter -= 2;
            }
            CANCEL.forcePlay(true, true);
         }
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE_RENDER">
      if (currentMode == MODE_RENDER) {
         if(animation == null || animation.isDone()){
            currentMode = MODE_CURSOR;
         }else{
            //Wait until animation completes
         }
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE_COMPUTER">
      else if (currentMode == MODE_COMPUTER) {
      }
      //</editor-fold>

      if ((trans & InputHelper.PAUSE) != 0) {
         leaving = true;
      }

      setDebugText();
      //throw new UnsupportedOperationException("Not supported yet.");
   }
   
   public void moveCursor(Point p){
      if(p != null){
         boolean mv = false;
         if(!(cx + p.x >= mapWidth || cx + p.x < 0) && p.x != 0){
            cx += p.x;
            mv = true;
         }

         if(!(cy + p.y >= mapHeight || cy + p.y < 0) && p.y != 0){
            cy += p.y;
            mv = true;
         }

         if(mv){
            moved = true;
            CURSOR.forcePlay(true, true);
         }else{
            ERROR.tryPlay(true, true);
         }
      }
   }
   
   public void moveRanges(Point p, int rangeSetter){
      if(rangeSetter < 0){
         moveCursor(p);
      }else if(p != null){
         boolean mv = false;
         if(p.x > 0){
            ranges[rangeSetter].up();
            mv = true;
         }else if(p.x < 0){
            ranges[rangeSetter].down();
            mv = true;
         }
         if(ranges.length > rangeSetter + 1){
            if(p.y < 0){
               ranges[rangeSetter + 1].up();
               mv = true;
            }else if(p.y > 0){
               ranges[rangeSetter + 1].down();
               mv = true;
            }
         }

         if(mv){
            moved = true;
            CURSOR.forcePlay(true, true);
         }else{
            ERROR.tryPlay(true, true);
         }
      }
   }
   
   public Point buttonMovement(int trans){
      int cx = 0, cy = 0;
      boolean moved = false;
      if ((trans & InputHelper.LEFT) != 0) {
         cx--;
         moved = true;
      }
      if ((trans & InputHelper.UP) != 0) {
         cy--;
         moved = true;
      }
      if ((trans & InputHelper.RIGHT) != 0) {
         cx++;
         moved = true;
      }
      if ((trans & InputHelper.DOWN) != 0) {
         cy++;
         moved = true;
      }
      if(moved){
         return new Point(cx, cy);
      }else{
         return null;
      }
   }
   
   public void clearSkillList(){
      skillList.clear();
   }
   
   public void setSkillListText(BattleEntity entity){
      if(entity != null){
         for(BattleSkill each: entity.getSkills()){
            skillList.put(new TextElement(each.getName(), skillList.getFont()));
         }
      }
   }
   
   public void setSkillListButton(BattleEntity entity){
      if(entity != null){
         for(BattleSkill tmp: entity.getSkills()){
            final BattleSkill each = tmp;
            skillList.put(new SelectableElement(each.getName(), skillList.getFont()) {

               @Override
               public void confirm() {
                  skill = each;
                  ranges = each.getAreaTemplateRanges();
                  rangeNames = each.getRangeNames();
                  rangeSetter = each.canMoveOrigin() ? -2 : 0;
                  currentMode = MODE_AOE;
               }
            });
         }
         this.selectDefault();
      }
   }

   public BattleEntity entityAtCursor() {
      return battlemap[cx][cy].getOccupant();
   }

   public int getMapWidth() {
      return mapWidth;
   }

   public int getMapHeight() {
      return mapHeight;
   }

   public Hex[][] getBattlemap() {
      return battlemap;
   }

   @Override
   public void release(KeyEvent e) {
      int trans = InputHelper.transform(e);

      //<editor-fold defaultstate="collapsed" desc="MODE_RENDER">
      if (currentMode == MODE_RENDER) {
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE_CURSOR">
      if (currentMode == MODE_CURSOR) {
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE_PLAYER">
      if (currentMode == MODE_PLAYER) {
      }
      //</editor-fold>

      //<editor-fold defaultstate="collapsed" desc="MODE_COMPUTER">
      if (currentMode == MODE_COMPUTER) {
      }
      //</editor-fold>

      //throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void render(Graphics2D g) {
      if(currentMode == MODE_RENDER){
         if(animation != null && !animation.isDone()){
            animation.preApply(g);
         }else{
            currentMode = MODE_CURSOR;
            animation = null;
         }
      }
      
      g.setColor(bgColor);
      g.fillRect(0, 0, AQEngine.getWidth(), AQEngine.getHeight());
      Stroke dStroke = g.getStroke();
      Stroke sStroke = new BasicStroke(3f);
      Hex h;
      int terr, rd, gr, bl, tx, ty;
      Color c, r;
      Shape s;
      BattleEntity actor;
      AreaTemplate selection = getAOE();
      float cos = (float) (Math.cos(cursorFrame / PERIOD * Math.PI * 2) / 2 + .5);
      float bty;
      int diffx = AQEngine.getWidth() / 2 - HEX_SIZE * 3 / 4;//(AQEngine.getWidth() - 2 * MENU_WIDTH - HEX_SIZE * 3 / 2 - 10) * cx) / mapWidth + 10 + MENU_WIDTH;
      int diffy = (AQEngine.getHeight() - MENU_HEIGHT - 10) / 2;//(AQEngine.getHeight() - MENU_HEIGHT - HEX_SIZE * 3 / 2 - 30) * cy) / mapHeight + 10;
      c = cursorGrad.get(cos);
      if(skill != null){
         r = skill.getGradient().get(cos);
      }else{
         r = offensiveGrad.get(cos);
      }
      for (int y = 0; y < mapHeight; y++) {
         ty = (y - cy) * HEX_SIZE * 3 / 4 + diffy + battlemap[cx][cy].zpos;
         for (int x = 0; x < mapWidth; x++) {
            h = battlemap[x][y];
            terr = h.getTerrain();
            tx = (x - cx) * HEX_SIZE + diffx;
            //            if ((y - cy) % 2 != 0) {
            //               tx += HEX_SIZE / 2 * (((y + mapHeight * 2) % 2) * 2 - 1);
            //            }
            if (y % 2 == 0) {
               tx += HEX_SIZE / 2;
            }
            //            drawHex(tx, ty, HEX_SIZE, terr, h.getZ(), 0, g);
            if (y < mapHeight - 1) {
               int min = battlemap[x][y + 1].zpos;
               if (x > 0) {
                  min = Math.min(battlemap[x - 1][y + 1].zpos, min);
               }
               if (x < mapWidth - 1) {
                  min = Math.min(battlemap[x + 1][y + 1].zpos, min);
               }
               if (x == 0 || x == mapWidth - 1) {
                  if (y < mapHeight - 2) {
                     min = Math.min(battlemap[x][y + 2].zpos - HEX_SIZE * 3 / 4, min);
                  } else {
                     min = battlemap[x][y].zpos - AQEngine.getHeight();
                  }
               }
               min = battlemap[x][y].zpos - min + HEX_SIZE / 4;
               min = Math.max(0, min);

               drawHex(tx, ty, HEX_SIZE, terr, h.getZ(), min, g);
            } else {
               drawHex(tx, ty, HEX_SIZE, terr, h.getZ(), AQEngine.getHeight(), g);
            }
            
            if(animation != null){
               animation.apply(g, tx, ty, h);
            }

            if (cx == x && cy == y && currentMode == MODE_CURSOR) {
               g.setColor(c);
               int ity = (int) (ty - h.getZ() - cos * HEX_SIZE / 2f - .5f);
               g.drawLine(tx, ity + HEX_SIZE / 4, tx + HEX_SIZE / 2, ity);
               g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE / 4, tx + HEX_SIZE / 2, ity);
            } else if (currentMode == MODE_AOE && selection != null && selection.contains(battlemap[x][y])) {
               g.setStroke(sStroke);
               g.setColor(r);
               int ity = (int) (ty - h.getZ() - cos * HEX_SIZE / 2f - .5f);
               g.drawLine(tx, ity + HEX_SIZE / 4, tx + HEX_SIZE / 2, ity);
               g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE / 4, tx + HEX_SIZE / 2, ity);
               g.setStroke(dStroke);
            }
            if ((actor = h.getOccupant()) != null) {
               actor.render(g, tx, ty - h.getZ() - HEX_SIZE / 3);
            }
         }
         if (currentMode == MODE_AOE) {
            g.setStroke(sStroke);
            g.setColor(r);
            for (int x = 0; x < mapWidth; x++) {
               h = battlemap[x][y];
               if (selection != null && selection.contains(h)) {
                  tx = (x - cx) * HEX_SIZE + diffx;
                  if (y % 2 == 0) {
                     tx += HEX_SIZE / 2;
                  }
                  int ity = (int) (ty - h.getZ() - cos * HEX_SIZE / 2f - .5f);
                  g.drawLine(tx, ity + HEX_SIZE * 3 / 4, tx + HEX_SIZE / 2, ity + HEX_SIZE);
                  g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE * 3 / 4, tx + HEX_SIZE / 2, ity + HEX_SIZE);
                  g.drawLine(tx, ity + HEX_SIZE / 4, tx, ity + HEX_SIZE * 3 / 4);
                  g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE / 4, tx + HEX_SIZE, ity + HEX_SIZE * 3 / 4);
               }
            }
            g.setStroke(dStroke);
         }
         if (cy == y && currentMode == MODE_CURSOR) {
            h = battlemap[cx][cy];
            tx = diffx;
            ty = diffy + h.zpos;
            if (y % 2 == 0) {
               tx += HEX_SIZE / 2;
            }
            g.setColor(c);
            bty = ty - h.getZ() - cos * HEX_SIZE / 2f - .5f;
            int ity = (int) bty;
            g.drawLine(tx, ity + HEX_SIZE * 3 / 4, tx + HEX_SIZE / 2, ity + HEX_SIZE);
            g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE * 3 / 4, tx + HEX_SIZE / 2, ity + HEX_SIZE);
            g.drawLine(tx, ity + HEX_SIZE / 4, tx, ity + HEX_SIZE * 3 / 4);
            g.drawLine(tx + HEX_SIZE, ity + HEX_SIZE / 4, tx + HEX_SIZE, ity + HEX_SIZE * 3 / 4);
         }
      }

      cursorFrame = (cursorFrame + 1) % PERIOD;
      if(animation != null){
         animation.postApply(g);
      }
      super.render(g);
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   public void drawHex(int px, int py, int size, int terr, int pz, int dropHeight, Graphics2D g) {
      Color c = cols[terr];
      py -= pz;
      if (dropHeight > 0) {
         Color d1 = c.darker();
         Color d2 = d1.darker();
         g.setColor(d1);
         g.fillRect(px, py + size * 3 / 4, size / 2 + 1, dropHeight);
         g.setColor(d2);
         g.fillRect(px + size / 2 + 1, py + size * 3 / 4, size / 2 - 1, dropHeight);
      }
      g.setColor(c);
      g.fillRect(px, py + size / 4, size, size / 2);
      for (int i = 0; i <= size / 4; i++) {
         g.fillRect(px + size / 2 - i * 2, py + i, 4 * i, 1);
         g.fillRect(px + size / 2 - i * 2, py + size - i, 4 * i, 1);
      }
      g.drawLine(px, py + size * 3 / 4, px + size / 2, py + size);
      g.drawLine(px + size, py + size * 3 / 4, px + size / 2, py + size);
      g.setColor(Color.BLACK);
      g.fillRect(px, py + size / 4, 1, dropHeight + size / 2);
      g.fillRect(px + size, py + size / 4, 1, dropHeight + size / 2);
      g.drawLine(px, py + size / 4, px + size / 2, py);
      g.drawLine(px + size, py + size / 4, px + size / 2, py);
   }

   private Color hexColor(int terr) {
      int rd, gr, bl;
      if ((terr & Hex.TERRAIN_WET) == 0) {
         //Not wet
         if ((terr & Hex.TERRAIN_GRASSY) == 0) {
            //Dirt
            //Brown
            rd = 190;
            gr = 120;
            bl = 50;
         } else {
            //Grass
            //Green
            rd = 30;
            gr = 120;
            bl = 0;
         }
      } else {
         //Wet
         if ((terr & Hex.TERRAIN_GRASSY) == 0) {
            //Dirt
            //Dark Brown
            rd = 120;
            gr = 70;
            bl = 30;
         } else {
            //Grass
            //Darker Bluish Green
            rd = 0;
            gr = 90;
            bl = 30;
         }
      }
      if ((terr & Hex.TERRAIN_BRIGHT) != 0) {
         //Brighten
         rd = Math.min(rd + 30, 255);
         gr = Math.min(gr + 30, 255);
         bl = Math.min(bl + 30, 255);
      }
      if ((terr & Hex.TERRAIN_DARK) != 0) {
         //Darken
         rd = Math.max(0, rd - 30);
         gr = Math.max(0, gr - 30);
         bl = Math.max(0, bl - 30);
      }
      rd = (int) Math.max(0, Math.min(rd + brightness, 255));
      gr = (int) Math.max(0, Math.min(gr + brightness, 255));
      bl = (int) Math.max(0, Math.min(bl + brightness, 255));
      return new Color(rd, gr, bl);
   }

   public void testAdjacents(int tx, int ty) {
      Hex[] adj = adjascents(tx, ty);
      System.out.println("Test for: " + tx + " / " + ty);
      for (int i = 0; i < adj.length; i++) {
         System.out.println("  " + i + ": " + adj[i].xpos + " / " + adj[i].ypos);
      }
      System.out.println();
   }

   public Hex[] adjascents(int x, int y) {
      Hex[] parts = new Hex[6];
      int i = 0;

      int shift = -y % 2;
      if (x > 0) {
         //Directly to the left
         parts[i] = battlemap[x - 1][y];
         i++;
      }
      if (x < mapWidth - 1) {
         //Directly to the right
         parts[i] = battlemap[x + 1][y];
         i++;
      }
      if (x + shift >= 0) {
         //To the left
         if (y > 0) {
            //And up
            parts[i] = battlemap[x + shift][y - 1];
            i++;
         }
         if (y < mapHeight - 1) {
            //And down
            parts[i] = battlemap[x + shift][y + 1];
            i++;
         }
      }
      if (x + shift + 1 < mapWidth) {
         //To the right
         if (y > 0) {
            //And up
            parts[i] = battlemap[x + shift + 1][y - 1];
            i++;
         }
         if (y < mapHeight - 1) {
            //And down
            parts[i] = battlemap[x + shift + 1][y + 1];
            i++;
         }
      }

      Hex[] temp = new Hex[i];
      for (i = 0; i < temp.length; i++) {
         temp[i] = parts[i];
      }
      return temp;
   }

   public static Point2D hexLoc(int x, int y) {
      double tx = x;
      double ty = y * 7. / 8;
      if (y % 2 == 0) {
         tx += .5;
      }
      return new Point2D.Double(tx, ty);
   }

   public static double hexDist(int x1, int y1, int x2, int y2) {
      double dx = x1 - x2;
      double dy = y1 - y2;
      if (y1 % 2 == 0) {
         dx += .5;
      }
      if (y2 % 2 == 0) {
         dx -= .5;
      }
      dy = dy * 7 / 8;
      return Math.sqrt(dx * dx + dy * dy);
   }

   public static double hexAng(int x1, int y1, int x2, int y2) {
      double dx = x1 - x2;
      double dy = y1 - y2;
      if (y1 % 2 == 0) {
         dx += .5;
      }
      if (y2 % 2 == 0) {
         dx -= .5;
      }
      dy = dy * 7 / 8;
      double out = Math.atan2(dy, dx);
      //System.out.println("ANGLE: ["+x1+"-"+x2+"]["+y1+"-"+y2+"]: "+dy+" / "+dx+" ~ "+out);
      return out;
   }

   @Override
   public void update() {
      super.update();
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   public static BattleMode makeAsymptoteMap(int points, double zRange, double mult, double bright) {
      BattleMode temp = zeroBattleMap(bright);
      class HeightPoint {

         double z;
         int x, y;

         HeightPoint(int tx, int ty, double tz) {
            z = tz;
            x = tx;
            y = ty;
         }
      }
      if (mult > 0) {
         HeightPoint[] pts = new HeightPoint[points];
         for (int i = 0; i < points; i++) {
            pts[i] = new HeightPoint(AQEngine.randInt(temp.mapWidth), AQEngine.randInt(temp.mapHeight), (AQEngine.randDouble() - .5) * zRange);
         }
         double dist, total;
         for (int i = 0; i < temp.mapWidth; i++) {
            for (int j = 0; j < temp.mapHeight; j++) {
               total = 0;
               for (int k = 0; k < points; k++) {
                  dist = hexDist(i, j, pts[k].x, pts[k].y) + mult;
                  total += pts[k].z * mult / dist;
               }
               temp.battlemap[i][j].zpos = (int) total;
            }
         }
      }
      temp.setDebugText();
      return temp;
   }

   public BattleMode setTerrainByHeight(double zRange) {
      for (int i = 0; i < mapWidth; i++) {
         for (int j = 0; j < mapWidth; j++) {
            if (this.battlemap[i][j].zpos < zRange / 2) {
               this.battlemap[i][j].terrain |= Hex.TERRAIN_GRASSY;
            } else {
               this.battlemap[i][j].terrain &= -1 - Hex.TERRAIN_GRASSY;
            }
            if (this.battlemap[i][j].zpos < -zRange / 2) {
               this.battlemap[i][j].terrain |= Hex.TERRAIN_WET;
            } else {
               this.battlemap[i][j].terrain &= -1 - Hex.TERRAIN_WET;
            }
         }
      }
      setDebugText();
      return this;
   }

   public static BattleMode smooth(BattleMode in) {
      Hex[] adj;
      int total;
      for (int i = 0; i < in.mapWidth; i++) {
         for (int j = 0; j < in.mapHeight; j++) {
            adj = in.adjascents(i, j);
            for (int k = 0; k < adj.length; k++) {
               in.battlemap[i][j].zpos += adj[k].zpos;
            }
            in.battlemap[i][j].zpos /= adj.length + 1;
         }
      }
      in.setDebugText();
      return in;
   }

   public BattleMode smooth() {
      return smooth(this);
   }
   
   public AreaTemplate getAOE(){
      if(skill != null){
         double tmp[] = new double[ranges.length];
         for(int i=0; i<tmp.length; i++){
            tmp[i] = ranges[i].getCurrent();
         }
         return skill.getAreaTemplate(this, cx, cy, tmp);
      }
      return null;
   }
   
   public double[] getAOEVals(){
      if(skill != null){
         double tmp[] = new double[ranges.length];
         for(int i=0; i<tmp.length; i++){
            tmp[i] = ranges[i].getCurrent();
         }
         return tmp;
      }
      return null;
   }

   public static BattleMode zeroBattleMap(double bright) {
      BattleMode temp = new BattleMode(bright);
      for (int i = 0; i < temp.mapWidth; i++) {
         for (int j = 0; j < temp.mapHeight; j++) {
            temp.battlemap[i][j].zpos = 0;
         }
      }
      temp.setDebugText();
      return temp;
   }
}
