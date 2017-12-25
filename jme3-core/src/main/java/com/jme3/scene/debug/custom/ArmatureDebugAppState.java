/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.debug.custom;

import com.jme3.anim.*;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.DirectionalLight;
import com.jme3.math.*;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;

import java.util.*;

/**
 * @author Nehon
 */
public class ArmatureDebugAppState extends BaseAppState {

    private Node debugNode = new Node("debugNode");
    private Map<Armature, ArmatureDebugger> armatures = new HashMap<>();
    private Map<Armature, Joint> selectedBones = new HashMap<>();
    private Application app;
    private boolean displayAllJoints = false;
    ViewPort vp;

    @Override
    protected void initialize(Application app) {
        vp = app.getRenderManager().createMainView("debug", app.getCamera());
        vp.attachScene(debugNode);
        vp.setClearDepth(true);
        this.app = app;
        for (ArmatureDebugger armatureDebugger : armatures.values()) {
            armatureDebugger.initialize(app.getAssetManager());
        }
        app.getInputManager().addListener(actionListener, "shoot", "toggleJoints");
        app.getInputManager().addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        app.getInputManager().addMapping("toggleJoints", new KeyTrigger(KeyInput.KEY_F10));

        debugNode.addLight(new DirectionalLight(new Vector3f(-1f, -1f, -1f).normalizeLocal()));

        debugNode.addLight(new DirectionalLight(new Vector3f(1f, 1f, 1f).normalizeLocal(), new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f)));
        vp.setEnabled(false);
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        vp.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        vp.setEnabled(false);
    }

    @Override
    public void update(float tpf) {
        debugNode.updateLogicalState(tpf);
        debugNode.updateGeometricState();
    }

    public ArmatureDebugger addArmatureFrom(SkinningControl skinningControl) {
        Armature armature = skinningControl.getArmature();
        Spatial forSpatial = skinningControl.getSpatial();
        return addArmatureFrom(armature, forSpatial);
    }

    public ArmatureDebugger addArmatureFrom(Armature armature, Spatial forSpatial) {

        JointInfoVisitor visitor = new JointInfoVisitor(armature);
        forSpatial.depthFirstTraversal(visitor);

        ArmatureDebugger ad = new ArmatureDebugger(forSpatial.getName() + "_Armature", armature, visitor.deformingJoints);
        ad.setLocalTransform(forSpatial.getWorldTransform());
        if (forSpatial instanceof Node) {
            List<Geometry> geoms = new ArrayList<>();
            findGeoms((Node) forSpatial, geoms);
            if (geoms.size() == 1) {
                ad.setLocalTransform(geoms.get(0).getWorldTransform());
            }
        }
        armatures.put(armature, ad);
        debugNode.attachChild(ad);
        if (isInitialized()) {
            ad.initialize(app.getAssetManager());
        }
        return ad;
    }

    private void findGeoms(Node node, List<Geometry> geoms) {
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeoms((Node) spatial, geoms);
            }
        }
    }

    /**
     * Pick a Target Using the Mouse Pointer. <ol><li>Map "pick target" action
     * to a MouseButtonTrigger. <li>flyCam.setEnabled(false);
     * <li>inputManager.setCursorVisible(true); <li>Implement action in
     * AnalogListener (TODO).</ol>
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("shoot") && isPressed) {
                CollisionResults results = new CollisionResults();
                Vector2f click2d = app.getInputManager().getCursorPosition();
                Vector3f click3d = app.getCamera().getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
                Vector3f dir = app.getCamera().getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d);
                Ray ray = new Ray(click3d, dir);

                debugNode.collideWith(ray, results);
                if (results.size() == 0) {
                    for (ArmatureDebugger ad : armatures.values()) {
                        ad.select(null);
                    }
                    return;
                }
                // The closest result is the target that the player picked:
                Geometry target = results.getClosestCollision().getGeometry();
                for (ArmatureDebugger ad : armatures.values()) {
                    Joint selectedjoint = ad.select(target);
                    if (selectedjoint != null) {
                        selectedBones.put(ad.getArmature(), selectedjoint);
                        System.err.println("-----------------------");
                        System.err.println("Selected Joint : " + selectedjoint.getName() + " in armature " + ad.getName());
                        System.err.println("Root Bone : " + (selectedjoint.getParent() == null));
                        System.err.println("-----------------------");
                        System.err.println("Local translation: " + selectedjoint.getLocalTranslation());
                        System.err.println("Local rotation: " + selectedjoint.getLocalRotation());
                        System.err.println("Local scale: " + selectedjoint.getLocalScale());
                        System.err.println("---");
                        System.err.println("Model translation: " + selectedjoint.getModelTransform().getTranslation());
                        System.err.println("Model rotation: " + selectedjoint.getModelTransform().getRotation());
                        System.err.println("Model scale: " + selectedjoint.getModelTransform().getScale());
                        System.err.println("---");
                        System.err.println("Bind inverse Transform: ");
                        System.err.println(selectedjoint.getInverseModelBindMatrix());
                        return;
                    }
                }
            }
            if (name.equals("toggleJoints") && isPressed) {
                displayAllJoints = !displayAllJoints;
                for (ArmatureDebugger ad : armatures.values()) {
                    ad.displayNonDeformingJoint(displayAllJoints);
                }
            }
        }
    };

//    public Map<Skeleton, Bone> getSelectedBones() {
//        return selectedBones;
//    }

    public Node getDebugNode() {
        return debugNode;
    }

    public void setDebugNode(Node debugNode) {
        this.debugNode = debugNode;
    }

    private class JointInfoVisitor extends SceneGraphVisitorAdapter {

        List<Joint> deformingJoints = new ArrayList<>();
        Armature armature;

        public JointInfoVisitor(Armature armature) {
            this.armature = armature;
        }

        @Override
        public void visit(Geometry g) {
            for (Joint joint : armature.getJointList()) {
                if (g.getMesh().isAnimatedByJoint(armature.getJointIndex(joint))) {
                    deformingJoints.add(joint);
                }
            }
        }
    }
}
