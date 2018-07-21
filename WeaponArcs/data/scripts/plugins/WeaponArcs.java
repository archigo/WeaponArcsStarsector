package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class WeaponArcs extends BaseEveryFrameCombatPlugin{
	
	private CombatEngineAPI engine;
    private ShipAPI player;
    private static Color WEAPON_ARC_COLOR;

	@Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        WEAPON_ARC_COLOR = Global.getSettings().getColor("weaponArcColor");
    }
	
	
	public void advance(float amount, List<InputEventAPI> events){
		
		if (engine == null || engine.getCombatUI() == null) {
            return;
        }

        if (engine.isUIShowingDialog())
            return;

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

        this.drawAllWeaponFacings();
        this.drawAllWeaponArcs();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
	}

	private static void glColor(Color color) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (41f));
    }

	private void drawAllWeaponFacings() {
		List<WeaponAPI> weapons = engine.getPlayerShip().getAllWeapons();

		for (WeaponAPI weapon : weapons) {
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
    }

    private void drawAllWeaponArcs() {
		List<WeaponAPI> weapons = engine.getPlayerShip().getAllWeapons();

		for (WeaponAPI weapon : weapons) {
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
    }

    private void drawArc(Vector2f center, Vector2f start, float range, int segments){
        Vector2f oldPoint = start;
        
        float rotation = range / segments;
        for (int i = 0;  i < segments; i++) {
            Vector2f newpoint = new Vector2f(0f,0f);
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
