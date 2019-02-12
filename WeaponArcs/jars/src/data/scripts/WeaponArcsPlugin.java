package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeaponArcsPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private ShipAPI player;
    private static Color WEAPON_ARC_COLOR;
    private static ArrayList<ArrayList<String>> DRAW_WEAPONS;
    private static String CurrentShip;
    private static final String CONFIG_PATH = "weapon-arcs-settings.json";
    private static final String PERSISTENT_WEAPONS_KEY = "weaponArcsPersistWeapons";
    private static final String PERSISTENT_SHIP_KEY = "weaponArcsPersistShipname";
    private JSONObject settings;
    private String persistedShipname;
    private Boolean firstRun = true;
    private ArrayList<Boolean> ActiveGroups;
    public static Logger log = Global.getLogger(WeaponArcsPlugin.class);
    

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        
        Map<String, Object> data = Global.getSector().getPersistentData();
        persistedShipname = (String) data.get(PERSISTENT_SHIP_KEY);
        
        DRAW_WEAPONS = new ArrayList<>();
        ActiveGroups = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            DRAW_WEAPONS.add(new ArrayList<String>());
            ActiveGroups.add(false);
        }
        
        // Id is new for each session, so workaround with name.
        CurrentShip = "";                                      
        
        try {
            settings = Global.getSettings().loadJSON(CONFIG_PATH);
            JSONArray colorArray = settings.getJSONArray("weaponArcColor");
            WEAPON_ARC_COLOR = new Color(Integer.parseInt(colorArray.get(0).toString()), 
                    Integer.parseInt(colorArray.get(1).toString()), 
                    Integer.parseInt(colorArray.get(2).toString()), 
                    Integer.parseInt(colorArray.get(3).toString()));
                           
            
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        //WEAPON_ARC_COLOR = Global.getSettings().getColor("weaponArcColor");
        
    }
    
    private void toogleAutoGroups(){
        if(settings.optBoolean("autoEnable1"))
                toggleWeaponGroup(0);
            if(settings.optBoolean("autoEnable2"))
                toggleWeaponGroup(1);
            if(settings.optBoolean("autoEnable3"))
                toggleWeaponGroup(2);
            if(settings.optBoolean("autoEnable4"))
                toggleWeaponGroup(3);
            if(settings.optBoolean("autoEnable5"))
                toggleWeaponGroup(4);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        
        if(firstRun && persistedShipname != null && engine.getPlayerShip().getName().equals(persistedShipname)){
            firstRun = false;
            log.info("Shipname: " + persistedShipname);
            CurrentShip = engine.getPlayerShip().getId();
            
            Map<String, Object> data = Global.getSector().getPersistentData();
            ArrayList<Boolean> persistedGroups = (ArrayList<Boolean>) data.get(PERSISTENT_WEAPONS_KEY);
            for (int i = 0; i < persistedGroups.size(); i++) {
                if(persistedGroups.get(i))
                    toggleWeaponGroup(i);                
            }            
        }           
        
        if (engine == null || engine.getCombatUI() == null) {
            return;
        }

        if (engine.isUIShowingDialog()) {
            return;
        }

        if (!engine.isSimulation() && !engine.isUIShowingHUD()) {
            return;
        }
        if (engine.getCombatUI().isShowingCommandUI()) {
            return;
        }

        player = engine.getPlayerShip();
        if (player == null || !engine.isEntityInPlay(player)) {
            return;
        }
        
        if(!CurrentShip.equals(engine.getPlayerShip().getId())){
            CurrentShip = engine.getPlayerShip().getId();
            for (ArrayList<String> arrayList : DRAW_WEAPONS) {
                arrayList.clear();
            }
            Map<String, Object> data = Global.getSector().getPersistentData();
            data.put(PERSISTENT_SHIP_KEY, engine.getPlayerShip().getName());
            toogleAutoGroups();
        }
        for (InputEventAPI event : events) {
            if (event.isAltDown()) {                
                switch (event.getEventChar()) {
                    case '1':
                        toggleWeaponGroup(0);
                        break;
                    case '2':
                        toggleWeaponGroup(1);
                        break;
                    case '3':
                        toggleWeaponGroup(2);
                        break;
                    case '4':
                        toggleWeaponGroup(3);
                        break;
                    case '5':
                        toggleWeaponGroup(4);
                        break;
                    default: break;
                }
            }
        }

        ViewportAPI viewport = engine.getViewport();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor()), height
                = (int) (Display.getHeight()
                * Display.getPixelScaleFactor());
        GL11.glViewport(0, 0, width, height);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
                viewport.getLLY() + viewport.getVisibleHeight(), -1,
                1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        this.handleDraw();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
    }

    private void toggleWeaponGroup(int index) {
        if (DRAW_WEAPONS.get(index).isEmpty()) {
            List<WeaponGroupAPI> weaponGroups = engine.getPlayerShip().getWeaponGroupsCopy();
            if(weaponGroups.size() <= index) return; // Ships has less than groups than index.
            List<WeaponAPI> weapons = weaponGroups.get(index).getWeaponsCopy();
            for (int i = 0; i < weapons.size(); i++) {
                WeaponAPI weapon = weapons.get(i);
                DRAW_WEAPONS.get(index).add(weapon.getSlot().toString());                
            }
            ActiveGroups.set(index, true);
        } else {            
            DRAW_WEAPONS.get(index).clear();       
            ActiveGroups.set(index, false);
        }
        Map<String, Object> data = Global.getSector().getPersistentData();
            data.put(PERSISTENT_WEAPONS_KEY, ActiveGroups);
    }

    private static void glColor(Color color) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (41f));
    }

    private void handleDraw() {
        List<WeaponAPI> weapons = engine.getPlayerShip().getAllWeapons();

        for (WeaponAPI weapon : weapons) {
            boolean skip = true;
            for (int i = 0; i < DRAW_WEAPONS.size(); i++) {
                for (int j = 0; j < DRAW_WEAPONS.get(i).size(); j++) {
                    if ( DRAW_WEAPONS.get(i).get(j).equals(weapon.getSlot().toString())) {
                        skip = false;
                    }
                }
            }
            if (skip || weapon.getType() == WeaponType.SYSTEM) {
                continue;
            }
            
            this.drawWeaponFacing(weapon);
            this.drawWeaponArc(weapon);
        }
    }

    @SuppressWarnings("static-access")
    private void drawWeaponFacing(WeaponAPI weapon) {

        if (!weapon.isDisabled()) {
            Vector2f location = weapon.getLocation();
            float cangle = weapon.getCurrAngle();
            Vector2f toRotate = new Vector2f(location.x + weapon.getRange(), location.y);
            Vector2f dest = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotate, location, cangle, dest);

            toRotate = new Vector2f(location.x + 5, location.y);
            Vector2f start = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotate, location, cangle, start);

            this.glColor(WEAPON_ARC_COLOR);

            this.drawLine(start, dest);
        }

    }

    @SuppressWarnings("static-access")
    private void drawWeaponArc(WeaponAPI weapon) {
        
        if (!weapon.isDisabled()) {
            Vector2f location = weapon.getLocation();
            float arc = weapon.getArc();
            float arcFacing = weapon.getArcFacing();
            float left = arcFacing - (arc / 2);
            float right = arcFacing + (arc / 2);
            Vector2f toRotateLeft = new Vector2f(location.x + weapon.getRange(), location.y);
            Vector2f destLeft = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotateLeft, location, left, destLeft);
            Vector2f toRotateRight = new Vector2f(location.x + weapon.getRange(), location.y);
            Vector2f destRight = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotateRight, location, right, destRight);

            float shipFacing = engine.getPlayerShip().getFacing();

            Vector2f finalLeft = new Vector2f(0, 0);
            Vector2f finalRight = new Vector2f(0, 0);

            VectorUtils.rotateAroundPivot(destLeft, location, shipFacing, finalLeft);
            VectorUtils.rotateAroundPivot(destRight, location, shipFacing, finalRight);

            Vector2f toRotateLeft2 = new Vector2f(location.x + 10, location.y);
            Vector2f destLeft2 = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotateLeft2, location, left, destLeft2);
            Vector2f toRotateRight2 = new Vector2f(location.x + 10, location.y);
            Vector2f destRight2 = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotateRight2, location, right, destRight2);

            Vector2f finalLeft2 = new Vector2f(0, 0);
            Vector2f finalRight2 = new Vector2f(0, 0);

            VectorUtils.rotateAroundPivot(destLeft2, location, shipFacing, finalLeft2);
            VectorUtils.rotateAroundPivot(destRight2, location, shipFacing, finalRight2);

            this.glColor(WEAPON_ARC_COLOR);

            this.drawLine(finalLeft2, finalLeft);
            this.drawLine(finalRight2, finalRight);

            int segments = (int) arc / 10;

            float startArc = right;

            float xdif = (finalLeft.x - location.x) / 4;
            float ydif = (finalLeft.y - location.y) / 4;

            this.drawArc(location,
                    finalLeft,
                    arc,
                    segments);

            this.drawArc(location,
                    new Vector2f(finalLeft.x - xdif, finalLeft.y - ydif),
                    arc,
                    segments);

            this.drawArc(location,
                    new Vector2f(finalLeft.x - xdif * 2, finalLeft.y - ydif * 2),
                    arc,
                    segments);

            this.drawArc(location,
                    new Vector2f(finalLeft.x - xdif * 3, finalLeft.y - ydif * 3),
                    arc,
                    segments);

        }
    }

    private void drawArc(Vector2f center, Vector2f start, float range, int segments) {
        Vector2f oldPoint = start;

        float rotation = range / segments;
        for (int i = 0; i < segments; i++) {
            Vector2f newpoint = new Vector2f(0f, 0f);
            VectorUtils.rotateAroundPivot(oldPoint, center, rotation, newpoint);
            this.drawLine(oldPoint, newpoint);
            oldPoint = newpoint;
        }
    }

    private void drawLine(Vector2f start, Vector2f end) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(start.x, start.y);
        GL11.glVertex2f(end.x, end.y);
        GL11.glEnd();
    }
}
