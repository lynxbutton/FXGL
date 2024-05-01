/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package sandbox.test3d;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.Camera3D;
import com.almasb.fxgl.scene3d.computingProject.CompressSoul;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import static com.almasb.fxgl.dsl.FXGL.*;
import static javafx.scene.transform.Rotate.*;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class Model3DSample extends GameApplication {

    private Camera3D camera3D;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.set3D(true);
    }

    @Override
    protected void initGame() {
        getGameScene().setBackgroundColor(Color.LIGHTCYAN);

        camera3D = getGameScene().getCamera3D();
        //camera3D.getTransform().translateY(-5);
        camera3D.getTransform().lookAt(Point3D.ZERO);

        // cube.obj is loaded from /assets/models/
        // cube.mtl (if exists) should also be located in the same directory
        //var model = getAssetLoader().loadModel3D("fox.obj");
        new CompressSoul().compress("fxgl-samples/src/main/resources/assets/models/fox2.dae");
        var model = getAssetLoader().loadModel3D("fox2.soul");
        //model.setMaterial(new PhongMaterial(Color.BLUE));

        // some models are tiny, so require scaling up
        model.setScaleX(55);
        model.setScaleY(55);
        model.setScaleZ(55);
        model.setRotationAxis(Y_AXIS);
        model.setRotate(90);
        //model.setRotationAxis(X_AXIS);
        //model.setRotate(0);


        entityBuilder()
                .view(model)
                .buildAndAttach();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
