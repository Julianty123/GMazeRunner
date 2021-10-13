import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.RadioButton;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import javax.swing.Timer;
import java.util.*;

@ExtensionInfo(
        Title = "GMazeRunner",
        Description = "It could be better",
        Version = "1.0.1",
        Author = "Julianty"
)

public class GMazeRunner extends ExtensionForm {
    public Button buttonGates;
    public RadioButton radioTileAvoid, radioButtonWalk, radioButtonRun;
    public CheckBox checkSwitch;

    public CheckBox checkTiles, checkGates;
    public Text textIndex,textCoords;

    // Key, Value
    //TreeMap<String,Integer> floorCoords_ID = new TreeMap<String, Integer>();
    TreeMap<Integer,HPoint> floorItemsID_HPoint = new TreeMap<>();

    public List<HPoint> coordTiles = new LinkedList<>();
    public List<Integer> listGates = new LinkedList<>();
    public List<Integer> switchFurnis = new LinkedList<>();
    public int k = 0;

    public int Id_Tile_Avoid;
    public String Words = null;
    public String YourName;
    public int YourIndex = -1;
    public int i, j;
    public int CurrentX, CurrentY;
    public int CoordClickX, CoordClickY;

    Timer timer1 = new Timer(1, e -> {
        //for(j = 0; j < floorItemsID_HPoint.size(); j++){
        for(j = 0; j < listGates.size(); j++){
            int GetXofGate = floorItemsID_HPoint.get(listGates.get(j)).getX();
            int GetYofGate = floorItemsID_HPoint.get(listGates.get(j)).getY();
            // ---Case example of coords --- //
            // UserCoord (3, 6); GateCoord (4, 6)
            if( CurrentX == GetXofGate - 1 && CurrentY == GetYofGate ){
                sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, listGates.get(j)));
            }
            // UserCoord (5, 6); GateCoord (4, 6)
            else if ( CurrentX == GetXofGate + 1 && CurrentY == GetYofGate ){
                sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, listGates.get(j)));
            }
            // UserCoord (4, 7); GateCoord (4, 6)
            else if ( CurrentX == GetXofGate && CurrentY == GetYofGate + 1){
                sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, listGates.get(j)));
            }
            // UserCoord (4, 5); GateCoord (4, 6)
            else if ( CurrentX == GetXofGate && CurrentY == GetYofGate - 1 ){
                sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, listGates.get(j)));
            }
        }
    });

    @Override
    protected void onShow() {   // Runs when you opens the extension
        YourIndex = -1;
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
        sendToServer(new HPacket("AvatarExpression", HMessage.Direction.TOSERVER, 0));

        // Get wallitems, flooritems and other things without restart room
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    @Override
    protected void onHide() {
        YourIndex = -1;
    }

    @Override
    protected void initExtension() {
        primaryStage.setOnShowing(s -> { // Start hook when you opens the extension
            // More information     https://github.com/kristian/system-hook
            // Might throw a UnsatisfiedLinkError if the native library fails to load or a RuntimeException if hooking fails
            GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(false); // Use false here to switch to hook instead of raw input
            keyboardHook.addKeyListener(new GlobalKeyAdapter() {
                @Override // Se ejecuta el tiempo que se mantenga presionada la tecla
                public void keyPressed(GlobalKeyEvent event) {
                    if(event.getVirtualKeyCode() == 37){ // Key Arrow left
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CurrentX - 1, CurrentY));
                    }
                    if(event.getVirtualKeyCode() == 38){ // Key Arrow up
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CurrentX, CurrentY - 1));
                    }
                    if(event.getVirtualKeyCode() == 39){ // Key Arrow right
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CurrentX + 1, CurrentY));
                    }
                    if(event.getVirtualKeyCode() == 40){ // Key Arrow down
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CurrentX, CurrentY + 1));
                    }
                    if(event.getVirtualKeyCode() == 120){ // Key f9
                        methodGate();
                    }
                    if(event.getVirtualKeyCode() == 119){ // Key f8
                        try {
                            sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, switchFurnis.get(k), 0));
                            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 9999, "You clicked the switch: " + (k+1)
                                    , 0, 19, 0, -1));
                            k++;
                        } catch (IndexOutOfBoundsException ignored){
                            k = 0;
                            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 9999,
                                    "The selector switch has been reset, so you will click on the first switch", 0, 19, 0, -1));
                        }
                    }
                }
            });
            // When you close the window the hook ends
            primaryStage.setOnCloseRequest(e -> keyboardHook.shutdownHook());
        });

        // Response of packet InfoRetrieve
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Gets Name and ID in order.
            int YourID = hMessage.getPacket().readInteger();
            YourName = hMessage.getPacket().readString();
        });

        // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            if(YourIndex == -1){ // this could avoid a bug
                YourIndex = hMessage.getPacket().readInteger();
                textIndex.setText("Index: " + YourIndex);  // GUI updated!
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "Chat", hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            String KeyWord = hMessage.getPacket().readString();
            try {
                if(CurrentIndex == YourIndex){
                    if(KeyWord.equalsIgnoreCase(":coordavoid")){
                        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Click where you want to click, thus " +
                                "avoiding the color tile", 0, 19, 0, -1)); hMessage.setBlocked(true);    Words = "CoordClick";
                    }
                    else if(KeyWord.equalsIgnoreCase(":deletetiles")){ // or .toLowerCase()
                        coordTiles.clear();
                        Platform.runLater(()->{
                            checkTiles.setText("Catch Tiles (" + coordTiles.size() + ")");
                        });
                        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "The tiles to walk to have been " +
                                "removed correctly", 0, 19, 0, -1)); hMessage.setBlocked(true);
                    }
                    else if(KeyWord.equalsIgnoreCase(":deletegates")){
                        listGates.clear();
                        Platform.runLater(()->{
                            checkGates.setText("Catch Gates (" + listGates.size() + ")");
                        });
                        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "The list of gates has been " +
                                "removed successfully", 0, 19, 0, -1));  hMessage.setBlocked(true);
                    }
                }
            }catch (Exception ignored){ }
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            HPacket hPacket = hMessage.getPacket(); // The data is added to a variable of type HPacket
            HPoint hPoint = new HPoint(hPacket.readInteger(), hPacket.readInteger());
            if(checkTiles.isSelected()){
                if (!coordTiles.contains(hPoint)){
                    coordTiles.add(hPoint);
                    Platform.runLater(() -> checkTiles.setText("Catch Tiles (" + coordTiles.size() + ")"));
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
            if(radioTileAvoid.isSelected()){
                hMessage.setBlocked(true); // Avoid walking by accident
            }
            if(Words == "CoordClick"){
                hMessage.setBlocked(true);
                CoordClickX = hPoint.getX();    CoordClickY = hPoint.getY();
                sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                        "Now, you will click when the tile isnt present in (" + CoordClickX + " ," + CoordClickY + " )"
                        , 0, 19, 0, -1));    //boolCoordClick = false; boolFireAvoid = true;
                Words = "FireAvoid";
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

        // Intercepts when a furni is moved from one place to another, for this case is a color tile
        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", hMessage -> {
            int oldX = hMessage.getPacket().readInteger();
            int oldY = hMessage.getPacket().readInteger();
            int newX = hMessage.getPacket().readInteger();
            int newY = hMessage.getPacket().readInteger();
            int NotUse = hMessage.getPacket().readInteger();
            int CurrentTileID = hMessage.getPacket().readInteger();
            if( Id_Tile_Avoid == CurrentTileID && Words == "FireAvoid"){
                /* Tienen que cumplirse muchas condiciones, las coordenadas de la baldosa color ser diferentes a donde
                se dara click y ademas el usuario debe estar en la posicion indicada */
                if( oldX != newX && oldY == newY ){ // When the color tile moves horizontally
                    if((newX != CoordClickX && newY == CoordClickY) &&
                            ((CurrentX == CoordClickX && CurrentY == CoordClickY + 1) ||
                                    (CurrentX == CoordClickX && CurrentY == CoordClickY - 1))){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CoordClickX, CoordClickY));
                    }
                }
                else if( oldX == newX && oldY != newY ){ // When the color tile moves vertically
                    if((newX == CoordClickX && newY != CoordClickY) &&
                            ((CurrentX == CoordClickX - 1 && CurrentY == CoordClickY ) ||
                                    (CurrentX == CoordClickX + 1 && CurrentY == CoordClickY ))){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, CoordClickX, CoordClickY));
                    }
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            if(radioTileAvoid.isSelected()){
                if( Id_Tile_Avoid == 0){
                    Id_Tile_Avoid = hMessage.getPacket().readInteger();
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                            "Color tile with id '" + Id_Tile_Avoid + "' added!", 0, 19, 0, -1));
                }
                else {
                    Id_Tile_Avoid = hMessage.getPacket().readInteger();
                    sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999,
                            "Old id has been replaced by this: " + Id_Tile_Avoid, 0, 19, 0, -1));
                }
                radioTileAvoid.setSelected(false);
            }
            if(checkSwitch.isSelected()){
                switchFurnis.add(hMessage.getPacket().readInteger());
                Platform.runLater(()-> checkSwitch.setText("Switch Furnis: " + switchFurnis.size()));
            }
        });

        // Se activa cuando el transpixelar es girado en este caso con Wired
        /* ignore this
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", hMessage -> {
            int FurniID = hMessage.getPacket().readInteger();
            int NotUse = hMessage.getPacket().readInteger();
            int CoordX = hMessage.getPacket().readInteger();
            int CoordY = hMessage.getPacket().readInteger();
            int Revolution = hMessage.getPacket().readInteger();
            //System.out.println("Rev: " + Revolution);
        });*/

        // Intercepts when you restart the room
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            listGates.clear();
            floorItemsID_HPoint.clear(); // Map deleted!
            HPacket hPacket = hMessage.getPacket();
            for (HFloorItem hFloorItem: HFloorItem.parse(hPacket)){
                HPoint hPoint = new HPoint(hFloorItem.getTile().getX(), hFloorItem.getTile().getY()); // Ignore z
                if(!floorItemsID_HPoint.containsKey(hFloorItem.getId())){ // Entra al condicional si no contiene la id especificada
                    // add furni just is in the range, uniqueid Aquamarine gate 2597 to  2605 Red gate ...
                    if(hFloorItem.getTypeId() >= 2597 && hFloorItem.getTypeId() <= 2605){
                        floorItemsID_HPoint.put(hFloorItem.getId(), hPoint);
                        listGates.add(hFloorItem.getId());
                    }
                }
                // 1636 Hela unique id // 125 unique id pod rojo // 38 unique id pod blanco
                /*if(!listGates.contains(hFloorItem.getId())){
                    if(hFloorItem.getTypeId() >= 2597 && hFloorItem.getTypeId() <= 2605){
                        listGates.add(hFloorItem.getId());
                    }
                }*/
            }
            Platform.runLater(()-> checkGates.setText("Catch Gates (" + listGates.size() + ")"));
        });

        // Intercept wall items but ignore this
        /*intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            for (HWallItem hWallItem: HWallItem.parse(hPacket)){
                // Agregar codigo en github para que se pueda obtener la coordenada x, y, w y h con un ciclo
                //System.out.println("ID Furni: " + hWallItem.getId());
                //System.out.println("x: " + hWallItem.getLocation());
            }
        });*/

        // Intercept this packet when you enter or restart a room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    // In another room, the index changes
                    if(hEntity.getName().equals(YourName)){
                        YourIndex = hEntity.getIndex();     k = 0; // Switches have been restarted
                        textIndex.setText("Index: " + YourIndex);  // Agrega el Index al GUI
                    }
                    /* Me permite agregar un valor al Map (Dictionary en c#) si este no se encuentra
                    ademas de remplazar si algun dato del par ha cambiado.
                    if(!NameAndIndex.containsKey(hEntity.getName())){
                        NameAndIndex.put(hEntity.getName(), hEntity.getIndex());
                    }
                    else { // Se especifica la key, para remplazar el value por uno nuevo
                        NameAndIndex.replace(hEntity.getName(), hEntity.getIndex());
                    }*/
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Sorry for the Spaghetti code, so i need to organize some things...
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            // La clase HEntityUpdate me permite obtener el index del usuario que esta caminando
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
                try {
                    int CurrentIndex = hEntityUpdate.getIndex();
                    if(YourIndex == CurrentIndex){
                        if(radioButtonWalk.isSelected()){
                            CurrentX = hEntityUpdate.getTile().getX();  CurrentY = hEntityUpdate.getTile().getY();
                        }
                        if(radioButtonRun.isSelected()){
                            CurrentX = hEntityUpdate.getMovingTo().getX();  CurrentY = hEntityUpdate.getMovingTo().getY();
                        }
                        /* Here, I was testing some things, so ignore this if you want
                        for (Map.Entry<Integer, String> entry : floorCoords_ID.entrySet()) {
                            if(CurrentX == entry.get && CurrentY == 11){

                            }
                            // System.out.println("key: " + entry.getKey() + ", value: " + entry.getValue());
                        }*/

                        /*
                        if(CurrentX == 10 & CurrentY == 12){
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                    10, 11));
                            System.out.println("hola funciona");
                        }

                        if(CurrentX == 10 & CurrentY == 11){
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                    11, 11));
                        }

                        if(CurrentX == 11 & CurrentY == 11){
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                    11, 12));
                        }

                        if(CurrentX == 11 & CurrentY == 12){
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                    10, 12));
                        }*/

                        textCoords.setText("Coords: ( " + CurrentX + ", " + CurrentY + " )");
                        for(i = 0; i < coordTiles.size(); i++){
                            if(CurrentX == coordTiles.get(i).getX() &&
                                    CurrentY == coordTiles.get(i).getY()){
                                try {
                                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER,
                                            coordTiles.get(i+1).getX(), coordTiles.get(i+1).getY()));
                                }catch (Exception e)
                                {
                                    coordTiles.clear();
                                    Platform.runLater(()->{
                                        checkTiles.setText("Catch Tiles (" + coordTiles.size() + ")");
                                    });
                                }
                            }
                        }
                        // Runs when user avoid the tile successfully
                        if(Objects.equals(Words, "FireAvoid") && ((CurrentX == CoordClickX && CurrentY == CoordClickY))){
                            Words = null;
                            Id_Tile_Avoid = 0;
                            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Congratulations! You have " +
                                    "passed successfully.", 0, 23, 0, -1));
                        }
                    }
                }
                catch (NullPointerException nullPointerException) { /* GetMovingTo() generates NullPointerException */ }
            }
        });
    }

    public void handleFire() {
        if(coordTiles.size() != 0){
            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, coordTiles.get(0).getX(), coordTiles.get(0).getY()));
        }
    }

    public void methodGate(){
        if("Disabled".equals(buttonGates.getText())){
            Platform.runLater(()-> buttonGates.setText("Enabled")); timer1.start();
        }
        else {
            Platform.runLater(()-> buttonGates.setText("Disabled")); timer1.stop();
        }
    }

    public void handleGates() {
        methodGate();
    }

    public void handleSwitch() {
        if(checkSwitch.isSelected()){
            Platform.runLater(()-> {
                checkSwitch.setText("Switch Furnis: 0");    switchFurnis.clear();
            }); k = 0;
        }
    }
}