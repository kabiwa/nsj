package agentj;

/**
 * The following OTcl procedures are used to set node attributes for NAM animations,
 * and are supported by this class.
 *
 * $node color [color] ;# sets color of node
 * $node shape [shape] ;# sets shape of node
 * $node label [label] ;# sets label on node
 * $node label-color [lcolor] ;# sets color of label
 * $node label-at [ldirection] ;# sets position of label
 * $node add-mark [name] [color] [shape] ;# adds a mark to node
 * $node delete-mark [name] ;# deletes mark from node
 *  
 * <p/>
 * Created by scmijt
 * Date: Jan 3, 2008
 * Time: 12:03:50 PM
 */
public class NAMCommands {
    private AgentJAgent agentJNode;

// This is the NS2 color definitions

    public enum NamColor {red, green, blue, chocolate, pink, brown, tan, gold, black, grey, magenta, purple};

    public enum NodeShape {circle, box, hexagon};

    public enum LabelPosition {up, down, left, right, upright, downright, upleft, downleft};

    private NAMCommands() {}
    
    /**
     * Constructus an interface to NAM commands for the particular AgentJ node
     *
     * @param agentJNode
     */
    public NAMCommands(AgentJAgent agentJNode) {
        this.agentJNode = agentJNode;
    }

    /**
     * Rate at which the animation proceeds at
     *
     * @param rate
     */
    public void setAnimationRate(double rate) {
        agentJNode.tclEvaluateOnSimulatorJava("set-animation-rate " + rate);
    }
    

    /**
     * Sets a shape for the node that this agent is attached to in NAM to one
     * of the following: hexagon, square
     *
     * @param shape
     *
     * @throws Exception if shape not found
     */
    public void setNodeShape(NodeShape shape) {
        String shapestr;
         if ( (shapestr=getShape(shape)) !=null)
             agentJNode.tclEvaluateOnAgentJava("shape " + shapestr);
    }

    /**
     * Puts text in the Nam trace section of the window (at the bottom)
     * @param text
     */
    public void traceAnnotate(String text) {
        agentJNode.tclEvaluateOnSimulatorJava("trace-annotate \"" + text + "\"");
    }


    /**
     * Adds a marker to an Ns2 node
     *
     * @param name id for the marker
     * @param color color of marker
     * @param shape shape for marker
     */
    public void addMark(String name, NamColor color, NodeShape shape) {
        String shapestr=getShape(shape);
        String colorstr = getColor(color);
        agentJNode.tclEvaluateOnAgentJava("add-mark " + name + " " + colorstr + " " + shapestr);
    }

    /**
     * Deletes mark with the given name
     *
     * @param name the name given to a previously added marker
     */
    public void deleteMark(String name) {
        agentJNode.tclEvaluateOnAgentJava("delete-mark " + name);
    }


    /**
     * Sets the label of the node to the provided name and orients it according to the
     * provided position and in the provided colour

     * @param labelText Text to display
     * @param position where to display it
     * @param color and on which color.
     */
    public void setNodeLabel(String labelText, LabelPosition position, NamColor color) {

        agentJNode.tclEvaluateOnAgentJava("label \"" + labelText + "\"");

        String pos;

        if ( (pos=getPosition(position)) !=null)
            agentJNode.tclEvaluateOnAgentJava("label-at " + pos);

        String colorstr;
        if ((colorstr = getColor(color)) != null)
            agentJNode.tclEvaluateOnAgentJava("label-color " + colorstr);
    }

    /**
     * Sets the color of the node
     * 
     * @param color
     */
    public void setNodeColor(NamColor color) {
        String colorstr;
        if ((colorstr = getColor(color)) != null)
            agentJNode.tclEvaluateOnAgentJava("color " + colorstr);
    }


    private String getColor(NamColor color) {
        switch (color) {
            case green: return "green";
            case red: return "red";
            case blue: return "blue";
            case chocolate: return "chocolate";
            case pink: return "pink";
            case brown: return "brown";
            case tan: return "tan";
            case gold: return "gold";
            case black: return "black";
            case magenta: return "magenta";
            case purple: return "purple";
        };
        return null; 
    }

    private String getPosition(LabelPosition pos) {
        switch (pos) {
            case up: return "up";
            case down: return "down";
            case left: return "left";
            case right: return "right";
            case upright: return "up-right";
            case downright: return "down-right";
            case upleft: return "up-left";
            case downleft: return "down-left";           
        };
        return null;
    }

    private String getShape(NodeShape shape) {
            switch (shape) {
                 case hexagon : return "hexagon";
                 case circle: return "circle";
                 case box: return "box";
                 default:
             };
        return null;
    }

}
