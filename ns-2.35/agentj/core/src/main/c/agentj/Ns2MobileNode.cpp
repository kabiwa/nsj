#include "Ns2MobileNode.h"
#include "config.h"
#include "mobilenode.h"
#include "wireless-phy.h"
#include "wireless-phyExt.h"

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     agentj_Ns2MobileNode
 * Method:    update_position
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_Ns2MobileNode_update_1position
  (JNIEnv *env, jobject obj){
    jclass Ns2MobileNode_class = env->GetObjectClass(obj);
    jfieldID ns2NodePtrFieldID = env->GetFieldID(Ns2MobileNode_class, "ns2NodePtr", "J");
    if (ns2NodePtrFieldID == NULL){
        printf("Ns2MobileNode: ns2NodePtrFieldID is null\n");
        abort();
    }

    jlong ns2NodePtr = env->GetLongField(obj, ns2NodePtrFieldID);
    if (ns2NodePtr == 0){ // if not yet set 
        // first get the agent
        jfieldID agentFieldID = env->GetFieldID(Ns2MobileNode_class, "agent", "Lagentj/AgentJAgent;");
        jobject agent = env->GetObjectField(obj, agentFieldID);
        if (agent == NULL){
            printf("Ns2MobileNode: agent is NULL\n");
            abort();
        }
 
        jclass agent_class = env->GetObjectClass(agent);
        if (agent_class == NULL){
            printf("agent_class is NULL\n");
            abort();
        }

        jmethodID getNodeTCLName_ID = env->GetMethodID(agent_class, "getNodeTCLName", "()Ljava/lang/String;");
        if (getNodeTCLName_ID == NULL){
            printf("Ns2MobileNode: getNodeTCLName_ID is NULL\n");
            abort();
        }
      
        // now call the method getNodeTCLName
        jstring nodeTclName = (jstring) env->CallObjectMethod(agent, getNodeTCLName_ID);
        const char* nodeTclName_char = env->GetStringUTFChars(nodeTclName, NULL);

        // look the node up in TCL
        Node* node = dynamic_cast<Node*>(TclObject::lookup(nodeTclName_char));
        if (node == NULL){
            printf("Ns2MobileNode: Could not load Node!\n");
            abort();
        }

        env->SetLongField(obj, ns2NodePtrFieldID, (jlong) node);
        ns2NodePtr = (jlong) node;
	env->ReleaseStringUTFChars(nodeTclName, nodeTclName_char);
    }

    MobileNode *mobileNode = dynamic_cast<MobileNode*>((Node*) ns2NodePtr);
    if (mobileNode == NULL){
        printf("Ns2MobileNode: Could not load MobileNode!\n");
        abort();
    }

    double X = mobileNode->X();
    double Y = mobileNode->Y();
    double Z = mobileNode->Z();

    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "X_", "D"), X);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "Y_", "D"), Y);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "Z_", "D"), Z);

    double speed = mobileNode->speed();
    double dX = mobileNode->dX();
    double dY = mobileNode->dY();
    double dZ = mobileNode->dZ();

    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "speed_", "D"), speed);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "dX_", "D"), dX);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "dY_", "D"), dY);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "dZ_", "D"), dZ);

    double destX = mobileNode->destX();
    double destY = mobileNode->destY();

    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "destX_", "D"), destX);
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "destY_", "D"), destY);

    double radius = mobileNode->radius();
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "radius_", "D"), radius);
   
    double position_update_time = mobileNode->getUpdateTime();
    env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "position_update_time_", "D"), position_update_time);


    const struct if_head& ifhead_ = mobileNode->ifhead();
    double highestZ = 0;
    Phy *n;
    Phy *tifp;
    for(n = ifhead_.lh_first; n; n = n->nextchnl()) {
           if(dynamic_cast<WirelessPhyExt*>(n)) {
                   if(((WirelessPhyExt *)n)->getAntennaZ() > highestZ){
                           highestZ = ((WirelessPhyExt *)n)->getAntennaZ();
                           tifp = n;
                   }
           } else if (dynamic_cast<WirelessPhy*>(n)) {
                   if(((WirelessPhy *)n)->getAntennaZ() > highestZ) {
                           tifp = n;
                           highestZ = ((WirelessPhy *)n)->getAntennaZ();
 		   }
           } else highestZ = 0;
    }

    float distance = -1;
    if (dynamic_cast<WirelessPhyExt*>(tifp)) {
           WirelessPhyExt *wifp = (WirelessPhyExt *)tifp;
           distance = wifp->getDist(wifp->getRXThresh(), wifp->getPt(), 1.0, 1.0,
                           highestZ , highestZ, wifp->getL(),
                           wifp->getLambda());
    } else if (dynamic_cast<WirelessPhy*>(tifp)) {
           WirelessPhy *wifp = (WirelessPhy *)tifp;
           distance = wifp->getDist(wifp->getRXThresh(), wifp->getPt(), 1.0, 1.0,
                           highestZ , highestZ, wifp->getL(),
                           wifp->getLambda());
    }

    if (distance != -1)
    	env->SetDoubleField(obj, env->GetFieldID(Ns2MobileNode_class, "radioDistance_", "D"), distance);

}

#ifdef __cplusplus
}
#endif
