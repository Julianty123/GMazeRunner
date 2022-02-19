import gearth.extensions.parsers.HPoint;

public class ColorTile {
    public int tileId;
    public HPoint tilePosition;
    public Object stateColor;     // Example: 0 = White, 1 = Yellow, 2 = Orange, 3 = Red, 4 = Pink, 5 = Blue, 6 = Green

    public ColorTile(int tileId, HPoint tilePosition, Object stateColor) {
        this.tileId = tileId;
        this.tilePosition = tilePosition;
        this.stateColor = stateColor;
    }

    public int getTileId() {
        return tileId;
    }

    public HPoint getTilePosition() {
        return tilePosition;
    }

    public void setTilePosition(HPoint tilePosition) {
        this.tilePosition = tilePosition;
    }

    public Object getStateColor() {
        return stateColor;
    }

    public void setStateColor(Object stateColor) { // Hay wired que cambian el color de las baldosas
        this.stateColor = stateColor;
    }

    public Integer stateColorByCoords (HPoint hPoint){
        return 1;
    }
}
