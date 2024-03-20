import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;
import java.util.Objects;

public class GMazeRunnerLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GMazeRunner.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.getScene().getStylesheets().add(Objects.requireNonNull(GMazeRunnerLauncher.class.getResource("GMazeRunner.css")).toExternalForm());
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.getScene().setFill(Color.TRANSPARENT);
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

//        Ugly way to set the icon (from IDE or from CMD is a headache) >:(
//        String pathName = "/C:/Users/DORADITO/IdeaProjects/MazeProgrammer/src/main/resources/imageJ.jfif";
//        File file = new File(pathName);
//        if (file.exists())
//            primaryStage.getIcons().add(new Image(file.toURI().toString()));

        // Best way to set the icon (Works in both IDE and compiled application)
        String resourceName = "imageJ.jfif";
        InputStream inputStream = GMazeRunnerLauncher.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {
            Image image = new Image(inputStream);
            primaryStage.getIcons().add(image);
        }

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GMazeRunnerLauncher.class);
    }
}
