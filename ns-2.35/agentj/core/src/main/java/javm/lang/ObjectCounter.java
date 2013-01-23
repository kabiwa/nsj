package javm.lang;


public class ObjectCounter {
    private int objectsWaiting;
    private java.lang.Object targetObject;
    private WaitInvocation waitInvocationObject;

    public ObjectCounter(int objectsWaiting, java.lang.Object targetObject) {
        this.objectsWaiting = objectsWaiting;
        this.targetObject = targetObject;
    }

    void increment() {
       ++objectsWaiting;
    }

    public java.lang.Object getTargetObject() {
        return targetObject;
    }

 
    public int getObjectsWaiting() {
        return objectsWaiting;
    }

    public WaitInvocation getWaitInvocationObject() {
        return waitInvocationObject;
    }

    public void setWaitInvocationObject(WaitInvocation waitInvocationObject) {
        this.waitInvocationObject=waitInvocationObject;
    }

    /**
     *
     * Decrements counter. Returns true if count is zero
     */
    boolean decrementandCheckZero() {
        if (objectsWaiting==0)
          return true;
        --objectsWaiting;
        if (objectsWaiting<=0)
            return true;
        else return false;
    }
}