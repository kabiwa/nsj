package agentj;

import agentj.thread.Controller;

/**
 * The Ns2MobileNode represents a mobile node in Ns-2
 *
 * Created by herberg
 * Date: Nov 12, 2008
 * Time: 12:29:41 PM
 */
public class Ns2MobileNode extends Ns2Node {
    private long ns2NodePtr;
    private double X_;
    private double Y_;
    private double Z_;
    private double speed_;
    private double dX_;
    private double dY_;
    private double dZ_;
    private double destX_;
    private double destY_;
    private double radius_;
    private double position_update_time_;
    private double radioDistance_;

    public Ns2MobileNode(String hostName, Controller controller) {
        super(hostName, controller);
    }

    /**
     * Calculates the distance between this node and the given node.
     * @param node the given node
     * @returns the distance
     **/
    public double distance(Ns2MobileNode node){
        update_position();
        node.update_position();
        return Math.sqrt(Math.pow(X_ - node.getX(), 2) + Math.pow(Y_ - node.getY(), 2) + Math.pow(Z_ - node.getZ(), 2));
    }

/*    public double propdelay(Ns2MobileNode node){
        update_position();
        return 0;
    }*/

    /**
     * Returns the current location of the node in a vector value[3].
     * @returns value[0] = X_location, value[1] = Y_location, value[2] = Z_location
     **/
    public double[] getLocation(){
        update_position();
        double[] loc = new double[3];
        loc[0] = X_; 
        loc[1] = Y_; 
        loc[2] = Z_;
        return loc;
    }

    /**
     * Returns the velocity vector of the node in a vector value[3].
     * @returns value[0] = X_speed, value[1] = Y_speed, value[2] = Z_speed
     **/
    public double[] getVelocity() {
        update_position();
        double[] velo = new double[3];
        velo[0] = dX_ * speed_; 
        velo[1] = dY_ * speed_; 
        velo[2] = 0.0;
        return velo;
    }
    
    /**
     * Returns the X value of the node position
     * @returns X
     */
    public double getX() {
        update_position();
        return X_;
    }

    /**
     * Returns the Y value of the node position
     * @returns Y
     */
    public double getY() {
        update_position();
        return Y_;
    }

    /**
     * Returns the Z value of the node position
     * @returns Z
     */
    public double getZ() {
        update_position();
        return Z_;
    }

    /**
     * Returns the absolute speed of the node
     * @returns speed
     */
    public double getSpeed() {
        update_position();
        return speed_;
    }

    /**
     * Returns the X value of the direction vector
     * @returns dX
     */
    public double getDX() {
        update_position();
        return dX_;
    }
    
    /**
     * Returns the Y value of the direction vector
     * @returns dY
     */
    public double getDY() {
        update_position();
        return dY_;
    }

    /**
     * Returns the dZ value of the direction vector
     * @returns dZ
     */
    public double getDZ() {
        update_position();
        return dZ_;
    }

    /**
     * Returns the X value of the destination point of the node
     * @returns destinationX
     */
    public double getDestinationX() {
        update_position();
        return destX_;
    }

    /**
     * Returns the Y value of the destination point of the node
     * @returns destinationY
     */
    public double getDestinationY() {
        update_position();
        return destY_;
    }


    /**
     * Returns the radio distance of the node (actually of its first interface)
     *
     */
    public double getRadioDistance(){
       update_position();
       return radioDistance_;
    }

    /**
     * Returns the radius of the node
     * @returns radius
     */
    public double getRadius() { 
        update_position();
        return radius_; 
    }

    /**
     * Returns the time when the position has been updated last
     * @returns time
     */
    public double getUpdateTime() {
        update_position();
        return position_update_time_; 
    }

    /**
     * Returns the X value of the node position
     * @returns X
     */
    public String toString(){
        update_position();
        StringBuffer buf = new StringBuffer();
        buf.append("MobileNode\t");
        buf.append(getHostName());
        buf.append(":\tX:\t");
        buf.append(String.format("%.9f", X_));
        buf.append(";\tY:\t");
        buf.append(String.format("%.9f", Y_));
        buf.append(";\tZ:\t");
        buf.append(String.format("%.9f", Z_));
        buf.append(";\tdX:\t");
        buf.append(String.format("%.9f", dX_));
        buf.append(";\tdY:\t");
        buf.append(String.format("%.9f", dY_));
        buf.append(";\tdZ:\t");
        buf.append(String.format("%.9f", dZ_));
        buf.append(";\tspeed:\t");
        buf.append(String.format("%.2f", speed_));
        return buf.toString();
    }

    protected native void update_position();
}
