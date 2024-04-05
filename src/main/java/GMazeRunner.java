/* import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;*/
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;       // Important library for use json!
import org.json.JSONArray;

import java.io.IOException;
import java.net.URL;
import javax.swing.Timer;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.LogManager;


@ExtensionInfo(
        Title = "GMazeRunner",
        Description = "It could be better",
        Version = "1.5.0",
        Author = "Julianty"
)

/*  Could be implemented in the future, idk
    Genetic Algorithm
    https://towardsdatascience.com/introduction-to-genetic-algorithms-including-example-code-e396e98d8bf3

    NEAT Algorithm
    https://github.com/vishnugh/evo-NEAT        any video as help: https://www.youtube.com/watch?v=vvSjJZDPQVc
    to import like maven dependency i think https://mvnrepository.com/artifact/com.vadeen/neat/0.2.0
 */

public class GMazeRunner extends ExtensionForm implements NativeKeyListener {
    public AnchorPane anchorPane;
    public RadioButton rbSwitchOff, rbSwitchAuto, rbSwitchKey,
            radioButtonOff, radioButtonAuto, radioButtonKey, radioButtonWalk, radioButtonRun; // Gates
    public CheckBox checkSwitch, checkCoords, checkGates, checkWalkToColorTile, checkThrough;
    public TextField textDelayGates, txtHotKeyGates, txtHotKeySwitches;
    public Text textIndex, textCoords;
    public Label labelHotKeyGates;
    public Text txtConnectedTo;
    public TextField txtAPI;
    public Button buttonTryConnect;
    TextInputControl lastInputControl = null;

    private HMessage _hMessage;
    public List<HPoint> listPositionTiles = new LinkedList<>();
    public List<Integer> listGates = new LinkedList<>();
    public List<Integer> listSwitches = new LinkedList<>();
    public List<ColorTile> listColorTiles = new LinkedList<>();

    public TreeMap<Integer,HPoint> floorItemsID_HPoint = new TreeMap<>();      // Key, Value
    public TreeMap<String, Integer> nameToTypeIdFloor = new TreeMap<>();

    public int idTileAvoid;
    public String flagWord = "", yourName;
    public int yourIndex = -1;
    public int i;
    public int currentX, currentY;
    public int clickPositionX, clickPositionY;
    public double xFrame, yFrame;

    public Timer timerGate = new Timer(1, e -> passGate());
    public Timer timerSwitch = new Timer(1, e -> hitSwitch());

    private static final Set<String> setGates = new HashSet<>(Arrays.asList("one_way_door*1", "one_way_door*2", "one_way_door*3",
            "one_way_door*4", "one_way_door*5", "one_way_door*6", "one_way_door*7", "one_way_door*8", "one_way_door*9", "onewaydoor_c22_rosegold"));
    private static final Set<String> setSwitches = new HashSet<>(Arrays.asList("wf_floor_switch1", "wf_floor_switch2"));
    private static final HashMap<String, String> hostToDomain = new HashMap<>();
    static {
        hostToDomain.put("game-es.habbo.com", "https://www.habbo.es/gamedata/furnidata_json/1");
        hostToDomain.put("game-br.habbo.com", "https://www.habbo.com.br/gamedata/furnidata_json/1");
        hostToDomain.put("game-tr.habbo.com", "https://www.habbo.com.tr/gamedata/furnidata_json/1");
        hostToDomain.put("game-us.habbo.com", "https://www.habbo.com/gamedata/furnidata_json/1");
        hostToDomain.put("game-de.habbo.com", "https://www.habbo.de/gamedata/furnidata_json/1");
        hostToDomain.put("game-fi.habbo.com", "https://www.habbo.fi/gamedata/furnidata_json/1");
        hostToDomain.put("game-fr.habbo.com", "https://www.habbo.fr/gamedata/furnidata_json/1");
        hostToDomain.put("game-it.habbo.com", "https://www.habbo.it/gamedata/furnidata_json/1");
        hostToDomain.put("game-nl.habbo.com", "https://www.habbo.nl/gamedata/furnidata_json/1");
        hostToDomain.put("game-s2.habbo.com", "https://sandbox.habbo.com/gamedata/furnidata_json/1");
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override   // Se ejecuta el tiempo que se mantenga presionada la tecla
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        String keyText = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        TextInputControl[] txtFieldsHotKeys = new TextInputControl[]{txtHotKeyGates, txtHotKeySwitches};

        for(TextInputControl element: txtFieldsHotKeys){
            if(element.isFocused()){    // si alguno de los controles tiene el control hace algo...
                element.setText(keyText);
                if(element.equals(txtHotKeyGates))
                    Platform.runLater(()-> radioButtonKey.setText(String.format("Key [%s]", keyText)));
                else if(element.equals(txtHotKeySwitches))
                    Platform.runLater(()-> rbSwitchKey.setText(String.format("key [%s]", keyText)));

                // lastInputControl = element;
                Platform.runLater(()-> labelHotKeyGates.requestFocus());    // Le da el foco al label :O
            }
            else if(!element.isFocused()){  // Si ninguno de los elementos tiene el foco...
                if(element.getText().equals(keyText)){
                    if(radioButtonKey.isSelected())
                        if(keyText.equals(txtHotKeyGates.getText()))
                            new Thread(this::passGate).start(); // When the key is released a new thread appears to stop the loop
                    if(rbSwitchKey.isSelected())
                        if(keyText.equals(txtHotKeySwitches.getText()))
                            new Thread(this::hitSwitch).start(); // When the key is released a new thread appears to stop the loop
                }
            }
        }
    }

    @Override // Se ejecuta cuando la tecla se deja de presionar
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        /* String keyText = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        TextInputControl[] txtFieldsHotKeys = new TextInputControl[]{txtHotKeyGates, txtHotKeySwitches};
        for(TextInputControl element: txtFieldsHotKeys) {
            if(element.equals(lastInputControl)){
                element.setText(keyText);   lastInputControl = null;
                break;
            }
        } */
    }

    public void userInitializer(){
        yourIndex = -1;
        // radioButtonAuto.setStyle("-fx-text-fill: cyan;");
        // textConnected.setFill(Paint.valueOf("BLUE")); // Example: "GREEN" or "#008000"

        sendToServer(new HPacket("{out:InfoRetrieve}")); // When its sent, get UserObject packet
        sendToServer(new HPacket("{out:AvatarExpression}{i:0}")); // When its sent, get UserIndex without restart room
    }

    @Override
    protected void onShow() {   // Runs when you opens the extension
        userInitializer();
        LogManager.getLogManager().reset();
        try {
            if(!GlobalScreen.isNativeHookRegistered()){
                GlobalScreen.registerNativeHook();
                System.out.println("Hook enabled");
            }
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }
        GlobalScreen.addNativeKeyListener(GMazeRunner.this);
    }

    @Override
    protected void onHide() {
        yourIndex = -1; checkThrough.setSelected(false);
        sendToClient(new HPacket(String.format("{in:YouArePlayingGame}{b:%b}", checkThrough.isSelected())));
        try {
            GlobalScreen.unregisterNativeHook();
            System.out.println("Hook disabled");
        } catch (NativeHookException | RejectedExecutionException nativeHookException) {
            nativeHookException.printStackTrace();
        }
        GlobalScreen.removeNativeKeyListener(this);
    }

    @Override
    protected void initExtension() {
        /*  primaryStage.setOnShowing(s -> {});
            primaryStage.setOnCloseRequest(e -> { });   */

        onConnect((host, port, APIVersion, versionClient, client) -> getGameData(host)); // host: game-es.habbo.com

        /* Cuando pasa el mouse por encima de un elemento, se cambia el color del texto
        radioButtonAuto.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                radioButtonAuto.setStyle("-fx-text-fill: blue;");
            }
            else{
                radioButtonAuto.setStyle("-fx-text-fill: black;");
            }
        }); */

        checkThrough.setOnAction(event ->
                sendToClient(new HPacket(String.format("{in:YouArePlayingGame}{b:%b}", checkThrough.isSelected()))));

        intercept(HMessage.Direction.TOCLIENT, "UserObject", this::interceptUserObject); // Response of packet InfoRetrieve
        intercept(HMessage.Direction.TOCLIENT, "Expression", this::interceptExpression); // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOSERVER, "Chat", this::interceptChat);
        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::interceptMoveAvatar);
        intercept(HMessage.Direction.TOSERVER, "EnterOneWayDoor", this::interceptEnterOneWayDoor);
        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::interceptSlideObjectBundle); // When a furniture is moved with wired

        // It should be intercepted when furniture is moved with wired... (Pending for update, of course!)
        intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::interceptWiredMovements);
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::interceptObjectUpdate); // When changes the coord of furniture with wired, i think
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", this::interceptUseFurniture);

        // Intercepts when a furniture change the state through wired (for example a color tile)
        intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::interceptObjectDataUpdate);
        intercept(HMessage.Direction.TOCLIENT, "Objects", this::interceptObjects); // Intercepts when you entry to the room

        intercept(HMessage.Direction.TOCLIENT, "Users", this::interceptUsers); // Intercept this packet when you enter or restart a room
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::interceptUserUpdate);
    }

    private void interceptUsers(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        HEntity[] roomUsersList = HEntity.parse(hPacket);
        for (HEntity hEntity: roomUsersList){
            try {
                if(hEntity.getName().equals(yourName)){ // In another room, the index changes
                    yourIndex = hEntity.getIndex();      // The userindex has been restarted
                    currentX = hEntity.getTile().getX();    currentY = hEntity.getTile().getY();
                    textIndex.setText("Index: " + yourIndex);
                }
                //System.out.println("stuff: " + Arrays.toString(hEntity.getStuff()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void interceptObjectDataUpdate(HMessage hMessage) {
        String furnitureId = hMessage.getPacket().readString();
        int idk = hMessage.getPacket().readInteger();
        String stateColor = hMessage.getPacket().readString();
        for(ColorTile colorTile: listColorTiles){
            if(colorTile.getTileId() == Integer.parseInt(furnitureId)){
                colorTile.setStateColor(stateColor); // Al cambiar el estado del color, actualiza los parametros de la lista!
                // System.out.println("id: " + colorTile.getTileId() + " ; stateColor: " + colorTile.getStateColor());
            }
        }
    }

    private void interceptObjectUpdate(HMessage hMessage) {
        int furnitureId = hMessage.getPacket().readInteger();
        hMessage.getPacket().readInteger();
        int xFurniture = hMessage.getPacket().readInteger();
        int yFurniture = hMessage.getPacket().readInteger();
        int revolution = hMessage.getPacket().readInteger();

        // There are mazes where the gates move with wired, so its necessary to update the map
        if (listGates.contains(furnitureId)) floorItemsID_HPoint.replace(furnitureId, new HPoint(xFurniture, yFurniture));
        if (listSwitches.contains(furnitureId)) floorItemsID_HPoint.replace(furnitureId, new HPoint(xFurniture,yFurniture));
    }

    private void interceptWiredMovements(HMessage hMessage) {
        // {in:WiredMovements}{i:2}{i:1}{i:12}{i:22}{i:12}{i:21}{s:"0.15"}{s:"0.15"}{i:783178091}{i:500}{i:0}{i:1}{i:14}{i:23}{i:15}{i:22}{s:"0.15001"}{s:"0.15001"}{i:1189005337}{i:500}{i:4}
        int count = hMessage.getPacket().readInteger();
        for(int i = 0; i < count; i++){
            try{
                int typeObject = hMessage.getPacket().readInteger(); // 0=User, 1=FloorItem, 2=WallItem, i think
                int oldX = hMessage.getPacket().readInteger();  int oldY = hMessage.getPacket().readInteger();
                int newX = hMessage.getPacket().readInteger();  int newY = hMessage.getPacket().readInteger();
                String oldZ = hMessage.getPacket().readString(); // I think that is this
                String newZ = hMessage.getPacket().readString();
                int idWiredMoving = hMessage.getPacket().readInteger();
                int animationTime = hMessage.getPacket().readInteger(); // I think x3
                int furnitureDirection = hMessage.getPacket().readInteger();

                // There are mazes where the gates move with wired, so its necessary to update the map
                if(listGates.contains(idWiredMoving)) floorItemsID_HPoint.replace(idWiredMoving, new HPoint(newX,newY));
                if(listSwitches.contains(idWiredMoving)) floorItemsID_HPoint.replace(idWiredMoving, new HPoint(newX,newY));
            } catch (Exception e){ e.printStackTrace(); }
        }
    }

    private void interceptEnterOneWayDoor(HMessage hMessage) {
        if(checkGates.isSelected()){
            int GateID = hMessage.getPacket().readInteger();
            if (!listGates.contains(GateID)){
                listGates.add(GateID);
                Platform.runLater(() -> checkGates.setText("Catch Gates (" + listGates.size() + ")"));
                sendToClient(new HPacket(String.format("{in:Chat}{i:999}{s:\"Gate with id '%s' added! \"}{i:0}{i:25}{i:0}{i:-1}", GateID)));
            }
        }
    }

    private void interceptChat(HMessage hMessage) {
        if(primaryStage.isShowing()){
            _hMessage = hMessage; // public variable
            String message = hMessage.getPacket().readString();
            if(message.equalsIgnoreCase(":tileavoid")){
                sendToClient(new HPacket("{in:Chat}{i:999}{s:\"Double click on the tile you want to avoid\"}{i:0}{i:19}{i:0}{i:-1}"));
                hMessage.setBlocked(true);
                flagWord = "TILEAVOID";   hMessage.setBlocked(true);
            }
            else if(message.equalsIgnoreCase(":deletecoords")) handleDeleteCoords();
            else if(message.equalsIgnoreCase(":deletegates")) handleDeleteGates();
            else if(message.equalsIgnoreCase(":deleteswitches")) handleDeleteSwitches();
        }
    }

    private void interceptExpression(HMessage hMessage) {
        // First integer is index in room, second is animation id, i think
        if(primaryStage.isShowing() && yourIndex == -1){ // this could avoid any bug
            yourIndex = hMessage.getPacket().readInteger();
            textIndex.setText("Index: " + yourIndex);
        }
    }

    private void interceptUserObject(HMessage hMessage){
        // Get name and id in order
        int YourID = hMessage.getPacket().readInteger();    yourName = hMessage.getPacket().readString();
    }

    private void interceptMoveAvatar(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket(); // The data is added to a variable of type HPacket
        HPoint hPoint = new HPoint(hPacket.readInteger(), hPacket.readInteger());
        if(checkCoords.isSelected()){
            if (!listPositionTiles.contains(hPoint)){
                listPositionTiles.add(hPoint);
                Platform.runLater(() -> checkCoords.setText("Catch Coords (" + listPositionTiles.size() + ")"));
                sendToClient(new HPacket(String.format("{in:Chat}{i:999}{s:\"Point (%s, %s) Added!\"}{i:0}{i:25}{i:0}{i:-1}", hPoint.getX(), hPoint.getY())));
            }
            hMessage.setBlocked(true);
        }
        if(checkGates.isSelected()){
            hMessage.setBlocked(true); // Avoid walking by accident when you are selecting the gate (Transpixelar)
            sendToClient(new HPacket("{in:Chat}{i:999}{s:\"Remember to deactivate this option once selected the gate/s\"}{i:0}{i:21}{i:0}{i:-1}"));
        }
        if(Objects.equals(flagWord, "TILEAVOID")){
            hMessage.setBlocked(true); // Avoid walking by accident
        }
        if(checkWalkToColorTile.isSelected()){
            for(ColorTile colorTile: listColorTiles){
                if (hPoint.getX() == colorTile.getTilePosition().getX() && hPoint.getY() == colorTile.getTilePosition().getY()) {
                    hMessage.setBlocked(false);
                    System.out.println(colorTile.getStateColor());
                    if(Objects.equals(colorTile.getStateColor(), 6)){ // 6 = GREEN
                        System.out.println("here should walk"); checkWalkToColorTile.setSelected(false);
                        hMessage.setBlocked(false);
                    }
                    else if(!Objects.equals(colorTile.getStateColor(), 6)){
                        System.out.println("se bloque caminar");
                        hMessage.setBlocked(true); // If the color is different to green, will NOT walk
                    }
                }
            }
                /*
                for(ColorTile colorTile: listColorTiles){
                    int coordXColorTile = colorTile.getTilePosition().getX();
                    int coordYColorTile = colorTile.getTilePosition().getY();
                    if ((currentX == coordXColorTile - 1 && currentY == coordYColorTile) || (currentX == coordXColorTile + 1 && currentY == coordYColorTile) ||
                            (currentX == coordXColorTile && currentY == coordYColorTile - 1) || (currentX == coordXColorTile && currentY == coordYColorTile + 1) ||
                            (currentX == coordXColorTile - 1 && currentY == coordYColorTile - 1) || (currentX == coordXColorTile + 1 && currentY == coordYColorTile + 1) ||
                            (currentX == coordXColorTile + 1 && currentY == coordYColorTile - 1) || (currentX == coordXColorTile - 1 && currentY == coordYColorTile + 1)) {
                    }
                }*/
        }
        if(Objects.equals(flagWord, "COORDCLICK")){
            hMessage.setBlocked(true);
            clickPositionX = hPoint.getX();    clickPositionY = hPoint.getY();
            sendToClient(new HPacket(String.format("{in:Chat}{i:999}{s:\"Now, you will click when the tile isnt present in (%s, %s)\"}{i:0}{i:19}{i:0}{i:-1}", clickPositionX, clickPositionY)));
            flagWord = "FIREAVOID";
        }
    }

    private void interceptSlideObjectBundle(HMessage hMessage) {
        int oldX = hMessage.getPacket().readInteger();
        int oldY = hMessage.getPacket().readInteger();
        int newX = hMessage.getPacket().readInteger();
        int newY = hMessage.getPacket().readInteger();
        int NotUse = hMessage.getPacket().readInteger();
        int idCurrentFurniMoving = hMessage.getPacket().readInteger();
        String furniElevation = hMessage.getPacket().readString();

        if (listGates.contains(idCurrentFurniMoving)) floorItemsID_HPoint.replace(idCurrentFurniMoving, new HPoint(newX,newY));

        for(ColorTile colorTile: listColorTiles){
            if(colorTile.getTileId() == idCurrentFurniMoving)
                colorTile.setTilePosition(new HPoint(newX, newY, Double.parseDouble(furniElevation))); // update the list parameters
        }

        if( idTileAvoid == idCurrentFurniMoving && Objects.equals(flagWord, "FIREAVOID")){
                /* Tienen que cumplirse muchas condiciones, las coordenadas de la baldosa color ser diferentes a donde
                se dara click yFrame ademas el usuario debe estar en la posicion indicada */
            if( oldX != newX && oldY == newY ){ // When the color tile moves horizontally
                if((newX != clickPositionX && newY == clickPositionY) &&
                        ((currentX == clickPositionX && currentY == clickPositionY + 1) ||
                                (currentX == clickPositionX && currentY == clickPositionY - 1))){
                    sendToServer(new HPacket(String.format("{out:MoveAvatar}{i:%s}{i:%s}", clickPositionX, clickPositionY)));
                }
            }
            else if( oldX == newX && oldY != newY ){ // When the color tile moves vertically
                if((newX == clickPositionX && newY != clickPositionY) &&
                        ((currentX == clickPositionX - 1 && currentY == clickPositionY) ||
                                (currentX == clickPositionX + 1 && currentY == clickPositionY))){
                    sendToServer(new HPacket(String.format("{out:MoveAvatar}{i:%s}{i:%s}", clickPositionX, clickPositionY)));
                }
            }
        }
    }

    private void interceptUseFurniture(HMessage hMessage) {
        int furnitureId = hMessage.getPacket().readInteger();
//         System.out.println(floorItemsID_HPoint.get(furnitureId));
        if(flagWord.equals("TILEAVOID")){
            if(idTileAvoid == 0){
                idTileAvoid = furnitureId;
                sendToClient(new HPacket(String.format("{in:Chat}{i:999}{s:\"Tile with id: '%s' added!\"}{i:0}{i:19}{i:0}{i:-1}", idTileAvoid)));
            }
            else {
                idTileAvoid = furnitureId;
                sendToClient(new HPacket(String.format("{in:Chat}{i:999}{s:\"Old id has been replaced by this: %s\"}{i:0}{i:19}{i:0}{i:-1}", idTileAvoid)));
            }
            sendToClient(new HPacket("{in:Chat}{i:999}{s:\"Click where you want to walk, that is to say, avoiding the color tile\"}{i:0}{i:19}{i:0}{i:-1}"));
            hMessage.setBlocked(true);    flagWord = "COORDCLICK";
        }
        if(checkSwitch.isSelected()){
            if(!listSwitches.contains(furnitureId)){
                listSwitches.add(furnitureId);
                Platform.runLater(()-> checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")"));
            }
            hMessage.setBlocked(true); // Block double click (accidentally walk)
        }
    }

    // Yet exists the Spaghetti code, i need to organize some things...
    public void interceptUserUpdate(HMessage hMessage){
        HPacket hPacket = hMessage.getPacket();
        for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
            try {
                int currentIndex = hEntityUpdate.getIndex();  // HEntityUpdate class allows get UserIndex
                if(yourIndex != currentIndex) continue;

                // fix bug roller (important), also update the coords when you entry to the room
                int jokerX = hEntityUpdate.getTile().getX();    int jokerY = hEntityUpdate.getTile().getY();
                if(radioButtonWalk.isSelected()){
                    currentX = hEntityUpdate.getTile().getX();  currentY = hEntityUpdate.getTile().getY();
                }
                else if(radioButtonRun.isSelected()){
                    currentX = jokerX;  currentY = jokerY;      // fix bug roller, because the coordinate is not updated
                    Platform.runLater(()-> textCoords.setText("Coords: ( " + jokerX + ", " + jokerY + " )"));   // fix bug roller (ignore)

                    currentX = hEntityUpdate.getMovingTo().getX();  currentY = hEntityUpdate.getMovingTo().getY();
                }
                Platform.runLater(()-> textCoords.setText("Coords: ( " + currentX + ", " + currentY + " )"));

                // Puedo agregar un listener en el futuro para poner delay al catch coords
                        /* SimpleObjectProperty<HPoint> simpleObjectProperty = new SimpleObjectProperty<>(new HPoint(currentX, currentY, 0));
                        simpleObjectProperty.addListener((observable, oldValue, newValue) -> {
                            // Here code when the variable changes
                        }); */

                for(i = 0; i < listPositionTiles.size(); i++){
                    if(currentX == listPositionTiles.get(i).getX() && currentY == listPositionTiles.get(i).getY()){
                        try {
                            sendToServer(new HPacket(String.format("{out:MoveAvatar}{i:%s}{i:%s}", listPositionTiles.get(i+1).getX(), listPositionTiles.get(i+1).getY())));
                        }catch (Exception e) {
                            listPositionTiles.clear();
                            Platform.runLater(()-> checkCoords.setText("Catch Coords (" + listPositionTiles.size() + ")"));
                        }
                    }
                }
                // Runs when user avoid the tile successfully
                if(Objects.equals(flagWord, "FIREAVOID") && ((currentX == clickPositionX && currentY == clickPositionY))){
                    flagWord = "";
                    idTileAvoid = 0;
                    sendToClient(new HPacket("{in:Chat}{i:999}{s:\"Congratulations! You have passed successfully.\"}{i:0}{i:23}{i:0}{i:-1}"));
                }
            }
            catch (NullPointerException ignored) {} // .getMovingTo() get null pointer exception
        }
    }

    public void interceptObjects(HMessage hMessage){
        if(checkThrough.isSelected()) sendToClient(new HPacket(String.format("{in:YouArePlayingGame}{b:%b}", true)));
        listGates.clear();  listSwitches.clear();   listColorTiles.clear(); floorItemsID_HPoint.clear();
        try{
            for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                // System.out.println("id: " + hFloorItem.getId() + " ; typeId: " + hFloorItem.getTypeId());
                HPoint hPoint = new HPoint(hFloorItem.getTile().getX(), hFloorItem.getTile().getY(), hFloorItem.getTile().getZ());
                if(!floorItemsID_HPoint.containsKey(hFloorItem.getId())){ // Entra al condicional si no contiene la id especificada
                    floorItemsID_HPoint.put(hFloorItem.getId(), hPoint);

                    for(String classNameGate: setGates){
                        // Check if there are those unique id or type id is in the room (This depends on the hotel you are connected)
                        if(hFloorItem.getTypeId() == nameToTypeIdFloor.get(classNameGate)) listGates.add(hFloorItem.getId());
                    }
                    for(String classNameSwitch: setSwitches){
                        if(hFloorItem.getTypeId() == nameToTypeIdFloor.get(classNameSwitch)) listSwitches.add(hFloorItem.getId());
                    }
                    if(hFloorItem.getTypeId() == nameToTypeIdFloor.get("wf_colortile")){
                            /*if( 5 == hPoint.getX() && (4 <= hPoint.getY() && 7 >= hPoint.getY())){ // Limita que furnis es para probar

                            }*/
                        // Object colorNumber = hFloorItem.getStuff()[0]; // Example: 0 = White, 1 = Yellow, 2 = Orange...
                        // listColorTiles.add(new ColorTile(hFloorItem.getId(), hPoint, colorNumber));
                    }
                }
            }
        }catch (Exception e) { System.out.println("Exception here!"); }
        Platform.runLater(()-> {
            checkGates.setText("Catch Gates (" + listGates.size() + ")");
            checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")");
            checkWalkToColorTile.setText("Walk to Color Tile (" + listColorTiles.size() + ")");
        });
    }

    public void handleFire() {
        if(!listPositionTiles.isEmpty())
            sendToServer(new HPacket(String.format("{out:MoveAvatar}{i:%s}{i:%s}", listPositionTiles.get(0).getX(), listPositionTiles.get(0).getY())));
    }

    // The server cannot be flooded with many packets or else they will be rejected, so the delay prevents that...
    public void Delay(){
        try { Thread.sleep(Integer.parseInt(textDelayGates.getText())); }
        catch (InterruptedException ignored) { }
    }

    public void handleDeleteGates() {
        listGates.clear();
        Platform.runLater(()-> checkGates.setText("Catch Gates (" + listGates.size() + ")"));
        sendToClient(new HPacket("{in:Chat}{i:999}{s:\"The list of gates has been removed successfully\"}{i:0}{i:19}{i:0}{i:-1}"));
        try{ _hMessage.setBlocked(true); }
        catch (NullPointerException ignored){}
    }

    public void handleDeleteCoords() {
        listPositionTiles.clear();
        Platform.runLater(()-> checkCoords.setText("Catch Coords (" + listPositionTiles.size() + ")"));
        sendToClient(new HPacket("{in:Chat}{i:999}{s:\"The coords to walk to have been removed correctly\"}{i:0}{i:19}{i:0}{i:-1}"));
        try{ _hMessage.setBlocked(true); }
        catch (NullPointerException ignored){}
    }

    public void handleDeleteSwitches() {
        listSwitches.clear();
        Platform.runLater(()-> checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")"));
        sendToClient(new HPacket("{in:Chat}{i:999}{s:\"The list of switches has been removed successfully\"}{i:0}{i:19}{i:0}{i:-1}"));
        try{ _hMessage.setBlocked(true); }
        catch (NullPointerException ignored){}
    }

    public void handleRadioButtonsGate() {
        if(radioButtonOff.isSelected()){
            if(timerGate.isRunning()) timerGate.stop();
        }
        else if(radioButtonAuto.isSelected()) timerGate.start();
        else if(radioButtonKey.isSelected()){
            if(timerGate.isRunning()) timerGate.stop();
        }
    }

    public void handleRadioButtonsSwitch(ActionEvent actionEvent) {
        if(rbSwitchOff.isSelected()){
            if(timerSwitch.isRunning()) timerSwitch.stop();
        }
        else if(rbSwitchAuto.isSelected()) timerSwitch.start();
        else if(rbSwitchKey.isSelected()){
            if(timerSwitch.isRunning()) timerSwitch.stop();
        }
    }

    public void handleTryConnect(){
        getGameData(null);
    }

    public void getGameData(String host) {
        new Thread(()->{
            try{
//                https://www.habbo.es/gamedata/furnidata_json/1 -> This link redirects
//                https://www.habbo.es/gamedata/furnidata_json/869b396d86c425d2244c23101660084e0dd992ff -> Info organized as a dictionary {}
//                https://www.habbo.es/gamedata/furnidata/4b7d10b21957494413fdf42d51eca53b88bc0e12 -> Info organized as a list []
//                https://www.habbo.es/gamedata/furnidata_xml/1 -> Info organized as XML <>

//                Example API from Retro: https://images.habblet.city/leet-asset-bundles/gamedata/habblet_furni.json

                buttonTryConnect.setDisable(true);
                System.out.println("Getting GameData...");

                String url;
                if(hostToDomain.get(host) == null)
                    url = txtAPI.getText();
                else
                    url = hostToDomain.get(host);

                txtAPI.setText(url);
                URLConnection connection = (new URL(url)).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                connection.connect();

                JSONObject jsonObj = new JSONObject(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));
                JSONArray floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
                floorJson.forEach(o -> {
                    JSONObject item = (JSONObject)o;
                    nameToTypeIdFloor.put(item.getString("classname"), item.getInt("id"));
                });

                Platform.runLater(()-> txtConnectedTo.setText("Connected to domain: " + url));
                System.out.println("Game-data Retrieved!");
                sendToServer(new HPacket("{out:GetHeightMap}")); // When its sent, get wallitems, flooritems and other things without restart room

                /* Code By Sirjonasxx
                JSONArray wallJson = object.getJSONObject("wallitemtypes").getJSONArray("furnitype");
                wallJson.forEach(o -> {
                        JSONObject item = (JSONObject)o;
                        nameToTypeidWall.put(item.getString("classname"), item.getInt("id"));
                        typeIdToNameWall.put(item.getInt("id"), item.getString("classname"));
                }); */
            }catch (IOException e){
                Platform.runLater(()-> txtConnectedTo.setText("Error: " + e.getMessage()));
            }

            buttonTryConnect.setDisable(false);
        }).start();
    }

    public void passGate(){
        try{
            for(Integer gateId: listGates){
                int xGate = floorItemsID_HPoint.get(gateId).getX();
                int yGate = floorItemsID_HPoint.get(gateId).getY();

                // Entry to conditional 'if' when (Example):
                // UserPosition (3, 6); GatePosition (4, 6) OR UserPosition (5, 6); GatePosition (4, 6)
                // ||
                // UserPosition (4, 7); GatePosition (4, 6) OR UserPosition (4, 5); GatePosition (4, 6)
                int diffX = Math.abs(currentX - xGate);
                int diffY = Math.abs(currentY - yGate);
                if ((diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1)) {
                    sendToServer(new HPacket(String.format("{out:EnterOneWayDoor}{i:%s}", gateId)));
                    Delay();
                }
            }
        }catch (ConcurrentModificationException ignored) {}
    }

    public void hitSwitch(){
        try{
            // The order of selection with the switches matters if they are on the sides, but applying a delay seems to solve it.
            for(Integer switchId: listSwitches){ // Iterate through Java List
                int xSwitch = floorItemsID_HPoint.get(switchId).getX();
                int ySwitch = floorItemsID_HPoint.get(switchId).getY();

                // Entry to conditional 'if' when (Example):
                // UserCoord (3, 6); SwitchCoord (4, 6) OR UserCoord (5, 6); SwitchCoord (4, 6)
                // ||
                // UserCoord (4, 5); SwitchCoord (4, 6) OR UserCoord (4, 7); SwitchCoord (4, 6)
                int diffX = Math.abs(currentX - xSwitch);
                int diffY = Math.abs(currentY - ySwitch);
                if ((diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1)) {
                    sendToServer(new HPacket(String.format("{out:UseFurniture}{i:%s}{i:0}", switchId)));
                    Delay();
                }

                // Entry to conditional 'if' when (Example):
                // UserCoord (3, 5); SwitchCoord (4, 6) OR UserCoord (5, 7); SwitchCoord (4, 6) [LEFT DIAGONAL]
                // ||
                // UserCoord (5, 5); SwitchCoord (4, 6) OR UserCoord (3, 7); SwitchCoord (4, 6) [RIGHT DIAGONAL]
                if (diffX == 1 && diffY == 1) {
                    sendToServer(new HPacket(String.format("{out:UseFurniture}{i:%s}{i:0}", switchId)));
                    Delay();
                }
            }
        /* Ignore this logic
            try {
                    sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, listSwitches.get(k), 0));
                        k++;
            } catch (IndexOutOfBoundsException ignored){
                k = 0;
            }
        */
        }catch (ConcurrentModificationException ignored) {}
    }

    public void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - xFrame);
        stage.setY(event.getScreenY() - yFrame);
    }

    public void handleMousePressed(MouseEvent event) {
        xFrame = event.getSceneX();
        yFrame = event.getSceneY();
    }

    public void handleClickClose(MouseEvent event) {
        Stage stage = (Stage) anchorPane.getScene().getWindow();
        stage.close();  onHide();   // fix bug
    }

    public void handleMouseMinimized(MouseEvent event) {
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }
}