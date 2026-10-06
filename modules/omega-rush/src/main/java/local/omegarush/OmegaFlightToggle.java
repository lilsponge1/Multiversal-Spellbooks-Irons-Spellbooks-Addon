package local.omegarush;

/** Two distinct jump presses within the vanilla seven-tick window. */
public final class OmegaFlightToggle {
    private boolean pressed;
    private int first=-1;

    public boolean update(boolean jump,int tick){
        boolean edge=jump&&!pressed;pressed=jump;
        if(first>=0&&(tick<first||tick-first>7))first=-1;
        if(!edge)return false;
        if(first>=0){first=-1;return true;}
        first=tick;return false;
    }

    public void reset(boolean jump){pressed=jump;first=-1;}
}
