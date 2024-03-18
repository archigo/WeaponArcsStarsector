package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeaponArcsPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI _engine;
    private ShipAPI _player;
    private Color _weaponArcColor;
    private Boolean _useGroupColors;
    private Integer _toggleKey;
    private static ArrayList<Color> _weaponArcGroupColors;
    private static ArrayList<ArrayList<String>> _DRAW_WEAPONS;
    private static String _currentShip;
    private static final String _CONFIG_PATH = "weapon-arcs-settings.json";
    private static final String _PERSISTENT_WEAPONS_KEY = "weaponArcsPersistWeapons";
    private static final String _PERSISTENT_SHIP_KEY = "weaponArcsPersistShipname";
    private JSONObject _settings;
    private Integer _arcRoughness;
    private Integer _rangeBands;
    private String _persistedShipname;
    private Boolean _firstRun = true;
    private Boolean _hasBeenInitialized = false;
    private ArrayList<Boolean> _activeGroups;
    public static Logger Log = Global.getLogger(WeaponArcsPlugin.class);

    @Override
    public void init(CombatEngineAPI engine) {
        try {
            this._engine = engine;

            Map<String, Object> data = Global.getSector().getPersistentData();
            _persistedShipname = (String) data.get(_PERSISTENT_SHIP_KEY);

            _DRAW_WEAPONS = new ArrayList<>();
            _activeGroups = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                _DRAW_WEAPONS.add(new ArrayList<String>());
                _activeGroups.add(false);
            }

            // Id is new for each session, so workaround with name.
            _currentShip = "";

            _settings = Global.getSettings().loadJSON(_CONFIG_PATH);
            _toggleKey = _settings.getInt("toggleKey");
            _weaponArcColor = parseColor("weaponArcColor");
            _useGroupColors = _settings.getBoolean("useCustomColorForEachGroup");
            if (_useGroupColors) {
                _weaponArcGroupColors = new ArrayList<Color>();
                for (int i = 1; i <= 7; i++) {
                    _weaponArcGroupColors.add(parseColor("weaponArcColor" + i));
                }
            }
            _arcRoughness = _settings.getInt("arcRoughness");
            if (_arcRoughness < 1) {
                _arcRoughness = 1;
            }
            _rangeBands = _settings.getInt("rangeBands");
            if (_rangeBands < 0) {
                _rangeBands = 0;
            }

            _hasBeenInitialized = true;
        } catch (Exception ex) {
            _hasBeenInitialized = false;
            Log.error(ex.getMessage());
        }
        // WEAPON_ARC_COLOR = Global.getSettings().getColor("weaponArcColor");

    }

    private void toogleAutoGroups() {
        if (_settings.optBoolean("autoEnable1"))
            toggleWeaponGroup(0);
        if (_settings.optBoolean("autoEnable2"))
            toggleWeaponGroup(1);
        if (_settings.optBoolean("autoEnable3"))
            toggleWeaponGroup(2);
        if (_settings.optBoolean("autoEnable4"))
            toggleWeaponGroup(3);
        if (_settings.optBoolean("autoEnable5"))
            toggleWeaponGroup(4);
        if (_settings.optBoolean("autoEnable6"))
            toggleWeaponGroup(5);
        if (_settings.optBoolean("autoEnable7"))
            toggleWeaponGroup(6);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (_hasBeenInitialized == false)
            return;

        if (_engine == null || _engine.getCombatUI() == null) {
            return;
        }

        if (_engine.isUIShowingDialog()) {
            return;
        }

        if (!_engine.isSimulation() && !_engine.isUIShowingHUD()) {
            return;
        }
        if (_engine.getCombatUI().isShowingCommandUI()) {
            return;
        }

        _player = _engine.getPlayerShip();

        if (_player == null || _player.getName() == null || !_engine.isEntityInPlay(_player)) {
            return;
        }

        if (_firstRun && _persistedShipname != null && _player.getName().equals(_persistedShipname)) {
            _firstRun = false;
            // log.info("Shipname: " + persistedShipname);
            _currentShip = _engine.getPlayerShip().getId();

            Map<String, Object> data = Global.getSector().getPersistentData();

            ArrayList<Boolean> persistedGroups = (ArrayList<Boolean>) data.get(_PERSISTENT_WEAPONS_KEY);
            if (persistedGroups != null) {
                for (int i = 0; i < persistedGroups.size(); i++) {
                    if (persistedGroups.get(i))
                        toggleWeaponGroup(i);
                }
            }

        }

        if (!_currentShip.equals(_engine.getPlayerShip().getId())) {
            _currentShip = _engine.getPlayerShip().getId();
            for (ArrayList<String> arrayList : _DRAW_WEAPONS) {
                arrayList.clear();
            }
            Map<String, Object> data = Global.getSector().getPersistentData();
            data.put(_PERSISTENT_SHIP_KEY, _engine.getPlayerShip().getName());
            toogleAutoGroups();
        }

        // Keyboard.isKeyDown(_toggleKey);

        // for (InputEventAPI event : events) {
        // if (event.isAltDown() || event.getEventChar() == _toggleKey) {
        // _isToggleOn = !_isToggleOn;
        // break;
        // }
        // }

        if (Keyboard.isKeyDown(_toggleKey)) {
            for (InputEventAPI event : events) {
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
                    case '6':
                        toggleWeaponGroup(5);
                        break;
                    case '7':
                        toggleWeaponGroup(6);
                        break;
                    default:
                        break;
                }
            }
        }

        ViewportAPI viewport = _engine.getViewport();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor()),
                height = (int) (Display.getHeight() * Display.getPixelScaleFactor());
        GL11.glViewport(0, 0, width, height);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
                viewport.getLLY() + viewport.getVisibleHeight(), -1, 1);

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
        if (_DRAW_WEAPONS.get(index).isEmpty()) {
            List<WeaponGroupAPI> weaponGroups = _engine.getPlayerShip().getWeaponGroupsCopy();
            if (weaponGroups.size() <= index)
                return; // Ships has less groups than index.
            List<WeaponAPI> weapons = weaponGroups.get(index).getWeaponsCopy();
            for (int i = 0; i < weapons.size(); i++) {
                WeaponAPI weapon = weapons.get(i);
                _DRAW_WEAPONS.get(index).add(weapon.getSlot().toString());
            }
            _activeGroups.set(index, true);
        } else {
            _DRAW_WEAPONS.get(index).clear();
            _activeGroups.set(index, false);
        }
        Map<String, Object> data = Global.getSector().getPersistentData();
        data.put(_PERSISTENT_WEAPONS_KEY, _activeGroups);
    }

    private static void glColor(Color color) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) (41f));
    }

    private void handleDraw() {
        List<WeaponAPI> weapons = _engine.getPlayerShip().getAllWeapons();

        for (int weaponIndex = 0; weaponIndex < weapons.size(); weaponIndex++) {
            WeaponAPI weapon = weapons.get(weaponIndex);
            boolean skip = true;
            Color color = _weaponArcColor;
            for (int i = 0; i < _DRAW_WEAPONS.size(); i++) {
                for (int j = 0; j < _DRAW_WEAPONS.get(i).size(); j++) {
                    if (_DRAW_WEAPONS.get(i).get(j).equals(weapon.getSlot().toString())) {
                        skip = false;
                        if (_useGroupColors) {
                            color = _weaponArcGroupColors.get(i);
                        }
                    }
                }
            }
            if (skip || weapon.getType() == WeaponType.SYSTEM) {
                continue;
            }

            if (_useGroupColors) {
                this.drawWeaponFacing(weapon, color);
                this.drawWeaponArc(weapon, color);
            } else {
                this.drawWeaponFacing(weapon, _weaponArcColor);
                this.drawWeaponArc(weapon, _weaponArcColor);
            }
        }
    }

    @SuppressWarnings("static-access")
    private void drawWeaponFacing(WeaponAPI weapon, Color color) {

        if (!weapon.isDisabled()) {
            Vector2f location = weapon.getLocation();
            float cangle = weapon.getCurrAngle();
            Vector2f toRotate = new Vector2f(location.x + weapon.getRange(), location.y);
            Vector2f dest = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotate, location, cangle, dest);

            toRotate = new Vector2f(location.x + 5, location.y);
            Vector2f start = new Vector2f(0, 0);
            VectorUtils.rotateAroundPivot(toRotate, location, cangle, start);

            this.glColor(color);

            this.drawLine(start, dest);
        }

    }

    @SuppressWarnings("static-access")
    private void drawWeaponArc(WeaponAPI weapon, Color color) {

        if (weapon.isDisabled()) {
            return;
        }

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

        float shipFacing = _engine.getPlayerShip().getFacing();

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

        this.glColor(color);

        this.drawLine(finalLeft2, finalLeft);
        //
        this.drawLine(finalRight2, finalRight);

        // How many parts to split the arch into, higher arcRoughness means less parts,
        // so a more blocky arc
        int segments = (int) arc / _arcRoughness;

        float xdif = (finalLeft.x - location.x) / (_rangeBands + 1);
        float ydif = (finalLeft.y - location.y) / (_rangeBands + 1);

        for (int i = 0; i < _rangeBands; i++) {
            this.drawArc(location, new Vector2f(finalLeft.x - (xdif * i), finalLeft.y - (ydif * i)), arc, segments);
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

    private Color parseColor(String jsonKey) throws Exception {
        JSONArray colorArray = _settings.getJSONArray(jsonKey);

        return new Color(Integer.parseInt(colorArray.get(0).toString()), Integer.parseInt(colorArray.get(1).toString()),
                Integer.parseInt(colorArray.get(2).toString()), Integer.parseInt(colorArray.get(3).toString()));
    }
}