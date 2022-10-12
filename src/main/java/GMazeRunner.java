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
import java.net.URL;
import javax.swing.Timer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.LogManager;


@ExtensionInfo(
        Title = "GMazeRunner",
        Description = "It could be better",
        Version = "1.4.8",
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
    public RadioButton radioButtonWalk, radioButtonRun, radioButtonOff, radioButtonAuto, radioButtonKey;
    public CheckBox checkSwitch, checkCoords, checkGates, checkWalkToColorTile, checkThrough;
    public TextField textDelayGates, txtHotKeyGates, txtHotKeySwitches;
    public Text textConnected, textIndex,textCoords;
    public Label labelHotKeyGates, labelSwitches;

    TextInputControl lastInputControl = null;

    private HMessage _hMessage;
    public List<HPoint> coordTiles = new LinkedList<>();
    public List<Integer> listGates = new LinkedList<>();
    public List<Integer> listSwitches = new LinkedList<>();
    public List<ColorTile> listColorTiles = new LinkedList<>();
    public AnchorPane anchorPane;

    TreeMap<Integer,HPoint> floorItemsID_HPoint = new TreeMap<>();      // Key, Value
    TreeMap<String, Integer> nameToTypeidFloor = new TreeMap<>();

    public String host;
    public int idTileAvoid;
    public String flagWord = "", yourName;
    public int yourIndex = -1;
    public int i;
    public int currentX, currentY;
    public int coordClickX, coordClickY;
    public double xFrame, yFrame;

    private static final Set<String> setGates = new HashSet<>(Arrays.asList("one_way_door*1", "one_way_door*2", "one_way_door*3",
            "one_way_door*4", "one_way_door*5", "one_way_door*6", "one_way_door*7", "one_way_door*8", "one_way_door*9"));
    private static final Set<String> setSwitches = new HashSet<>(Arrays.asList("wf_floor_switch1", "wf_floor_switch2"));
    private static final TreeMap<String, String> codeToDomainMap = new TreeMap<>();
    static {
        codeToDomainMap.put("br", ".com.br");
        codeToDomainMap.put("de", ".de");
        codeToDomainMap.put("es", ".es");
        codeToDomainMap.put("fi", ".fi");
        codeToDomainMap.put("fr", ".fr");
        codeToDomainMap.put("it", ".it");
        codeToDomainMap.put("nl", ".nl");
        codeToDomainMap.put("tr", ".com.tr");
        codeToDomainMap.put("us", ".com");
    }

    Timer timerGate = new Timer(1, e -> passGate());

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override   // Se ejecuta el tiempo que se mantenga presionada la tecla
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        String keyText = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        TextInputControl[] txtFieldsHotKeys = new TextInputControl[]{txtHotKeyGates, txtHotKeySwitches};
        /* When the key is released, somehow the loop stops, however it reduces performance and fails sometimes, sorry :/
        new Thread(() -> { }).start();*/
        for(TextInputControl element: txtFieldsHotKeys){
            if(element.isFocused()){    // si alguno de los controles tiene el control hace algo...
                element.setText(keyText);
                if(element.equals(txtHotKeyGates)){
                    Platform.runLater(()-> radioButtonKey.setText(String.format("Key [%s]", keyText)));
                }
                else if(element.equals(txtHotKeySwitches)){
                    Platform.runLater(()-> labelSwitches.setText(String.format("Press key [%s] to give double click in the switch", keyText)));
                }
                // lastInputControl = element;
                Platform.runLater(()-> labelHotKeyGates.requestFocus());    // Le da el foco al label :O
            }
            else if(!element.isFocused()){  // Si ninguno de los elementos tiene el foco...
                if(element.getText().equals(keyText)){
                    if(radioButtonKey.isSelected()){
                        if(keyText.equals(txtHotKeyGates.getText())){
                            try { passGate(); } catch (Exception ignored) { }
                        }
                    }
                    if(keyText.equals(txtHotKeySwitches.getText())){
                        handleSwitch();
                    }
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

    @Override
    protected void onShow() {   // Runs when you opens the extension
        yourIndex = -1;
        textConnected.setText("Connected to domain: " + codeToDomainMap.get(host));
        radioButtonAuto.setStyle("-fx-text-fill: blue;");
        // textConnected.setFill(Paint.valueOf("BLUE")); // Example: "GREEN" or "#008000"

        // When its sent, get UserObject packet
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
        // When its sent, get UserIndex without restart room
        sendToServer(new HPacket("AvatarExpression", HMessage.Direction.TOSERVER, 0));
        // When its sent, get wallitems, flooritems and other things without restart room
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));

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
        yourIndex = -1;     checkThrough.setSelected(false);
        sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, checkThrough.isSelected()));
        //GlobalScreen.removeNativeKeyListener(this);
        try {
            GlobalScreen.unregisterNativeHook();
            System.out.println("Hook disabled");
        } catch (NativeHookException | RejectedExecutionException nativeHookException) {
            nativeHookException.printStackTrace();
        }
        GlobalScreen.removeNativeKeyListener(this);
    }

    @Override
    protected void onStartConnection() {
        new Thread(() -> {
            System.out.println("Getting GameData...");
            try { getGameFurniData(); } catch (Exception ignored) { }
            System.out.println("Gamedata Retrieved!");
        }).start();
    }

    @Override
    protected void initExtension() {
        /*  primaryStage.setOnShowing(s -> {});
            primaryStage.setOnCloseRequest(e -> { });   */

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);   // Example: Of "game-es.habbo.com" only takes "es"
        });

        /* Cuando pasa el mouse por encima de un elemento, se cambia el color del texto
        radioButtonAuto.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                radioButtonAuto.setStyle("-fx-text-fill: blue;");
            }
            else{
                radioButtonAuto.setStyle("-fx-text-fill: black;");
            }
        });*/

        checkThrough.setOnAction(event ->{
            if(checkThrough.isSelected()){
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));
            }
            else {
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
            }
        });

        // Response of packet InfoRetrieve
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Gets Name and ID in order.
            int YourID = hMessage.getPacket().readInteger();
            yourName = hMessage.getPacket().readString();
        });

        // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            if(primaryStage.isShowing() && yourIndex == -1){ // this could avoid any bug
                yourIndex = hMessage.getPacket().readInteger();
                textIndex.setText("Index: " + yourIndex);  // GUI updated!
            }
        });

        intercept(HMessage.Direction.TOSERVER, "Chat", hMessage -> {
            if(primaryStage.isShowing()){
                _hMessage = hMessage; // public variable
                String message = hMessage.getPacket().readString();
                if(message.equalsIgnoreCase(":tileavoid")){
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Double click on the tile you want to avoid", 0, 19, 0, -1)); hMessage.setBlocked(true);
                    flagWord = "TILEAVOID";   hMessage.setBlocked(true);
                }
                else if(message.equalsIgnoreCase(":deletecoords")){  // or .toLowerCase()
                    handleDeleteCoords();
                }
                else if(message.equalsIgnoreCase(":deletegates")){
                    handleDeleteGates();
                }
                else if(message.equalsIgnoreCase(":deleteswitches")){
                    handleDeleteSwitches();
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            HPacket hPacket = hMessage.getPacket(); // The data is added to a variable of type HPacket
            HPoint hPoint = new HPoint(hPacket.readInteger(), hPacket.readInteger());
            if(checkCoords.isSelected()){
                if (!coordTiles.contains(hPoint)){
                    coordTiles.add(hPoint);
                    Platform.runLater(() -> checkCoords.setText("Catch Coords (" + coordTiles.size() + ")"));
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Point ( "
                            + hPoint.getX() + "," + hPoint.getY() + " ) Added!", 0, 25, 0, -1));
                }
                hMessage.setBlocked(true);
            }
            if(checkGates.isSelected()){
                hMessage.setBlocked(true); // Avoid walking by accident when you are selecting the gate (Transpixelar)
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                        "Remember to deactivate this option once selected the gate/s", 0, 21, 0, -1));
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
                coordClickX = hPoint.getX();    coordClickY = hPoint.getY();
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                        "Now, you will click when the tile isnt present in (" + coordClickX + " ," + coordClickY + " )", 0, 19, 0, -1));
                flagWord = "FIREAVOID";
            }
        });

        intercept(HMessage.Direction.TOSERVER, "EnterOneWayDoor", hMessage -> {
            if(checkGates.isSelected()){
                int GateID = hMessage.getPacket().readInteger();
                if (!listGates.contains(GateID)){
                    listGates.add(GateID);
                    Platform.runLater(() -> checkGates.setText("Catch Gates (" + listGates.size() + ")"));
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Gate with id '"
                            + GateID + "' added!", 0, 25, 0, -1));
                }
            }
        });

        // Intercepts when a furni is moved from one place to another with wired, for example a color tile
        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", hMessage -> {
            int oldX = hMessage.getPacket().readInteger();
            int oldY = hMessage.getPacket().readInteger();
            int newX = hMessage.getPacket().readInteger();
            int newY = hMessage.getPacket().readInteger();
            int NotUse = hMessage.getPacket().readInteger();
            int idCurrentFurniMoving = hMessage.getPacket().readInteger();
            String furniElevation = hMessage.getPacket().readString();

            if (listGates.contains(idCurrentFurniMoving)){ // There are mazes where the gates move with wired
                floorItemsID_HPoint.replace(idCurrentFurniMoving, new HPoint(newX,newY)); // Updates the map (Very important)
            }

            for(ColorTile colorTile: listColorTiles){
                if(colorTile.getTileId() == idCurrentFurniMoving){
                    colorTile.setTilePosition(new HPoint(newX, newY, Double.parseDouble(furniElevation))); // Se actualizan los parametros de la lista
                }
            }

            if( idTileAvoid == idCurrentFurniMoving && Objects.equals(flagWord, "FIREAVOID")){
                /* Tienen que cumplirse muchas condiciones, las coordenadas de la baldosa color ser diferentes a donde
                se dara click yFrame ademas el usuario debe estar en la posicion indicada */
                if( oldX != newX && oldY == newY ){ // When the color tile moves horizontally
                    if((newX != coordClickX && newY == coordClickY) &&
                            ((currentX == coordClickX && currentY == coordClickY + 1) ||
                                    (currentX == coordClickX && currentY == coordClickY - 1))){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, coordClickX, coordClickY));
                    }
                }
                else if( oldX == newX && oldY != newY ){ // When the color tile moves vertically
                    if((newX == coordClickX && newY != coordClickY) &&
                            ((currentX == coordClickX - 1 && currentY == coordClickY) ||
                                    (currentX == coordClickX + 1 && currentY == coordClickY))){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, coordClickX, coordClickY));
                    }
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            int furniId = hMessage.getPacket().readInteger();
            // System.out.println(floorItemsID_HPoint.get(furniId));
            if(flagWord.equals("TILEAVOID")){
                if(idTileAvoid == 0){
                    idTileAvoid = furniId;
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                            "Tile with id '" + idTileAvoid + "' added!", 0, 19, 0, -1));
                }
                else {
                    idTileAvoid = furniId;
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                            "Old id has been replaced by this: " + idTileAvoid, 0, 19, 0, -1));
                }
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Click where you want to walk, that is to say, " +
                        "avoiding the color tile", 0, 19, 0, -1)); hMessage.setBlocked(true);    flagWord = "COORDCLICK";
            }
            if(checkSwitch.isSelected()){
                if(!listSwitches.contains(furniId)){
                    listSwitches.add(furniId);
                    Platform.runLater(()-> checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")"));
                }
                hMessage.setBlocked(true); // Block double click (accidentally walk)
            }
        });

        // Intercepts when a furni change the state through wired (In this case a color tile)
        intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", hMessage -> {
            String furniId = hMessage.getPacket().readString();
            int idk = hMessage.getPacket().readInteger();
            String stateColor = hMessage.getPacket().readString();
            for(ColorTile colorTile: listColorTiles){
                if(colorTile.getTileId() == Integer.parseInt(furniId)){
                    colorTile.setStateColor(stateColor); // Al cambiar el estado del color, actualiza los parametros de la lista!
                    // System.out.println("id: " + colorTile.getTileId() + " ; stateColor: " + colorTile.getStateColor());
                }
            }
        });

        /* ignore this, Se activa cuando el transpixelar es girado en este caso con Wired
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", hMessage -> {
            int FurniID = hMessage.getPacket().readInteger();
            int NotUse = hMessage.getPacket().readInteger();
            int CoordX = hMessage.getPacket().readInteger();
            int CoordY = hMessage.getPacket().readInteger();
            int Revolution = hMessage.getPacket().readInteger();
            //System.out.println("Rev: " + Revolution);
        });*/

        // Intercepts when you loads the floor items (happens when you entry to the room)
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            if(checkThrough.isSelected()){ sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true)); }
            try{
                listGates.clear();  listSwitches.clear();   listColorTiles.clear();     // Lists deleted!
                floorItemsID_HPoint.clear(); // Map deleted!
                for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                    // System.out.println("id: " + hFloorItem.getId() + " ; typeId: " + hFloorItem.getTypeId());
                    HPoint hPoint = new HPoint(hFloorItem.getTile().getX(), hFloorItem.getTile().getY(), hFloorItem.getTile().getZ());
                    if(!floorItemsID_HPoint.containsKey(hFloorItem.getId())){ // Entra al condicional si no contiene la id especificada
                        floorItemsID_HPoint.put(hFloorItem.getId(), hPoint);
                        // Mirar si se puede usar forEach o algo asi...
                        for(String classNameGate: setGates){
                            // Check if there are those unique id or type id is in the room (This depends on the hotel you are connected)
                            if(hFloorItem.getTypeId() == nameToTypeidFloor.get(classNameGate)){
                                listGates.add(hFloorItem.getId());
                            }
                        }
                        for(String classNameSwitch: setSwitches){
                            if(hFloorItem.getTypeId() == nameToTypeidFloor.get(classNameSwitch)){
                                listSwitches.add(hFloorItem.getId());
                            }
                        }
                        if(hFloorItem.getTypeId() == nameToTypeidFloor.get("wf_colortile")){
                            /*if( 5 == hPoint.getX() && (4 <= hPoint.getY() && 7 >= hPoint.getY())){ // Limita que furnis es para probar

                            }*/
                            Object colorNumber = hFloorItem.getStuff()[0]; // Example: 0 = White, 1 = Yellow, 2 = Orange...
                            listColorTiles.add(new ColorTile(hFloorItem.getId(), hPoint, colorNumber));
                        }
                    }
                }
            }catch (Exception e) { System.out.println("Exception here!"); }
            Platform.runLater(()-> {
                checkGates.setText("Catch Gates (" + listGates.size() + ")");
                checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")");
                checkWalkToColorTile.setText("Walk to Color Tile (" + listColorTiles.size() + ")");
            });
        });

        // Intercept this packet when you enter or restart a room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            try {
                HEntity[] roomUsersList = HEntity.parse(hMessage.getPacket());
                for (HEntity hEntity: roomUsersList){
                    if(hEntity.getName().equals(yourName)){    // In another room, the index changes
                        yourIndex = hEntity.getIndex();      // The userindex has been restarted
                        currentX = hEntity.getTile().getX();    currentY = hEntity.getTile().getY();
                        textIndex.setText("Index: " + yourIndex);  // Add UserIndex to GUI
                    }
                    //System.out.println("stuff: " + Arrays.toString(hEntity.getStuff()));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        // Sorry for the Spaghetti code, so i need to organize some things...
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
                try {
                    int CurrentIndex = hEntityUpdate.getIndex();  // HEntityUpdate class allows get UserIndex
                    if(yourIndex == CurrentIndex){
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

                        for(i = 0; i < coordTiles.size(); i++){
                            if(currentX == coordTiles.get(i).getX() &&
                                    currentY == coordTiles.get(i).getY()){
                                try {
                                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                            coordTiles.get(i+1).getX(), coordTiles.get(i+1).getY()));
                                }catch (Exception e)
                                {
                                    coordTiles.clear();
                                    Platform.runLater(()-> checkCoords.setText("Catch Coords (" + coordTiles.size() + ")"));
                                }
                            }
                        }
                        // Runs when user avoid the tile successfully
                        if(Objects.equals(flagWord, "FIREAVOID") && ((currentX == coordClickX && currentY == coordClickY))){
                            flagWord = "";
                            idTileAvoid = 0;
                            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Congratulations! You have " +
                                    "passed successfully.", 0, 23, 0, -1));
                        }
                    }
                }
                catch (NullPointerException ignored) {} // .getMovingTo() get null pointer exception
            }
        });
    }

    public void handleFire() {
        if(coordTiles.size() != 0){
            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, coordTiles.get(0).getX(), coordTiles.get(0).getY()));
        }
    }

    // The server cannot be flooded with many packets or else they will be rejected, so the delay prevents that...
    public void Delay(){
        try {
            Thread.sleep(Integer.parseInt(textDelayGates.getText()));
        } catch (InterruptedException ignored) { }
    }

    public void handleDeleteGates() {
        listGates.clear();
        Platform.runLater(()-> checkGates.setText("Catch Gates (" + listGates.size() + ")"));
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "The list of gates has been " + "removed successfully", 0, 19, 0, -1));
        try{
            _hMessage.setBlocked(true);
        }catch (NullPointerException ignored){}
    }

    public void handleDeleteCoords() {
        coordTiles.clear();
        Platform.runLater(()-> checkCoords.setText("Catch Coords (" + coordTiles.size() + ")"));
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "The coords to walk to have been removed correctly", 0, 19, 0, -1));
        try{
            _hMessage.setBlocked(true);
        }catch (NullPointerException ignored){}
    }

    public void handleDeleteSwitches() {
        listSwitches.clear();
        Platform.runLater(()-> checkSwitch.setText("Switch Furnis (" + listSwitches.size() + ")"));
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "The list of switches has been removed successfully", 0, 19, 0, -1));
        try{
            _hMessage.setBlocked(true);
        }catch (NullPointerException ignored){}
    }

    public void handleRadioButtonsGate() {
        if(radioButtonOff.isSelected()){
            if(timerGate.isRunning()){ timerGate.stop();}
        }
        else if(radioButtonAuto.isSelected()){
            timerGate.start();
        }
        else if(radioButtonKey.isSelected()){
            if(timerGate.isRunning()){ timerGate.stop();}
        }
    }

    public void getGameFurniData() throws Exception{
        /*  https://www.habbo.es/gamedata/furnidata/1 -> Este link redirecciona a
            https://www.habbo.es/gamedata/furnidata/4b7d10b21957494413fdf42d51eca53b88bc0e12 -> Info organizado como lista []
            https://www.habbo.es/gamedata/furnidata_json/1 -> Info organizada como diccionario {}
            https://www.habbo.es/gamedata/furnidata_xml/1 -> Info organizada como XML <>   */

        String str = "https://www.habbo%s/gamedata/furnidata_json/68a1492edadcaf02fff3bfe0ddb4cf308e077774";
        JSONObject jsonObj = new JSONObject(IOUtils.toString(new URL(String.format(str, codeToDomainMap.get(host))).openStream(), StandardCharsets.UTF_8));
        JSONArray floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
        floorJson.forEach(o -> {
            JSONObject item = (JSONObject)o;
            nameToTypeidFloor.put(item.getString("classname"), item.getInt("id"));
        });
        /* Code By Sirjonasxx
        JSONArray wallJson = object.getJSONObject("wallitemtypes").getJSONArray("furnitype");
        wallJson.forEach(o -> {
                JSONObject item = (JSONObject)o;
                nameToTypeidWall.put(item.getString("classname"), item.getInt("id"));
                typeIdToNameWall.put(item.getInt("id"), item.getString("classname"));
        }); */
    }

    public void passGate(){
        try{
            for(Integer gateId: listGates){ // for(j = 0; j < floorItemsID_HPoint.size(); j++)
                int GetXofGate = floorItemsID_HPoint.get(gateId).getX(); // floorItemsID_HPoint.get(listGates.get(gateId)).getX();
                int GetYofGate = floorItemsID_HPoint.get(gateId).getY();

                // ---Case example of coords --- //
                // UserCoord (3, 6); GateCoord (4, 6)
                if( currentX == GetXofGate - 1 && currentY == GetYofGate ){
                    sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, gateId)); // listGates.get(gateId)
                    Delay();
                }
                // UserCoord (5, 6); GateCoord (4, 6)
                else if ( currentX == GetXofGate + 1 && currentY == GetYofGate ){
                    sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, gateId));
                    Delay();
                }
                // UserCoord (4, 7); GateCoord (4, 6)
                else if ( currentX == GetXofGate && currentY == GetYofGate + 1 ){
                    sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, gateId));
                    Delay();
                }
                // UserCoord (4, 5); GateCoord (4, 6)
                else if ( currentX == GetXofGate && currentY == GetYofGate - 1 ){
                    sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, gateId));
                    Delay();
                }
            }
        }catch (ConcurrentModificationException ignored) {}
    }

    public void handleSwitch(){
        // El orden de seleccion con los interruptores importa si se encuentran pegadas, pero aplicar un delay parece solucionarlo
        for(Integer switchId: listSwitches){ // Iterate through Java List
            int coordXSwitch = floorItemsID_HPoint.get(switchId).getX();
            int coordYSwitch = floorItemsID_HPoint.get(switchId).getY();

            // ---Case example of coords --- //
            // UserCoord (3, 6); SwitchCoord (4, 6) OR UserCoord (5, 6); SwitchCoord (4, 6)
            if((currentX == coordXSwitch - 1 && currentY == coordYSwitch) || (currentX == coordXSwitch + 1 && currentY == coordYSwitch)){
                sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, switchId, 0));
                Delay();
            }
            // UserCoord (4, 5); SwitchCoord (4, 6) OR UserCoord (4, 7); SwitchCoord (4, 6)
            else if((currentX == coordXSwitch && currentY == coordYSwitch - 1) || (currentX == coordXSwitch && currentY == coordYSwitch + 1)){
                sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, switchId, 0));
                Delay();
            }
            // UserCoord (3, 5); SwitchCoord (4, 6) OR UserCoord (5, 7); SwitchCoord (4, 6) [LEFT DIAGONAL]
            else if((currentX == coordXSwitch - 1 && currentY == coordYSwitch - 1) || (currentX == coordXSwitch + 1 && currentY == coordYSwitch + 1)){
                sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, switchId, 0));
                Delay();
            }
            // UserCoord (5, 5); SwitchCoord (4, 6) OR UserCoord (3, 7); SwitchCoord (4, 6) [RIGHT DIAGONAL]
            else if((currentX == coordXSwitch + 1 && currentY == coordYSwitch - 1) || (currentX == coordXSwitch - 1 && currentY == coordYSwitch + 1)){
                sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, switchId, 0));
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