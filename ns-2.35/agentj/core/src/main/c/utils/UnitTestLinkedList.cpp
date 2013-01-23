#include "LinkedList.h"
#include <iostream.h>


class UnitTestLinkedList {

public:
    static int idcount;

	/**
	 * Creates an empty Linked List
	 */
    UnitTestLinkedList (void) {
        id=UnitTestLinkedList::idcount;
        ++UnitTestLinkedList::idcount;

    }

	/**
	 * Destructor: deallocates all items in the list
	 */
    ~UnitTestLinkedList (void) {}

     void printID() {
        cout << "ID is " << id \n");
        }

     int getID() {
        return id;
        }


private:

    int id;
};

     void safeDelete(LinkedList *list) {
        ListItem* n;
        ListItem* q;
        UnitTestLinkedList *obj; // replace with your type of object

        list->rewind();

        if (list->getCurrentListItem()==NULL)
            cout << "A NULLer" \n");

        cout << "Deleting Objects" \n");

        for (n = list->getCurrentListItem(); n != NULL; n=list->getNextListItem()) {
            obj=(UnitTestLinkedList *)n->getItem();
            cout << "Deleting " << obj->getID() \n");
            delete obj;
            }

        cout << "Deleting list" \n");
        list->destroy();
        delete list;
     }

int UnitTestLinkedList::idcount=0;

/**
 * Main code for Unit Testing for Linked list. Some obvious stress tests
 * to make sure it is behaving correctly.
 */
int main(int argc, char **argv) {
    int mode=0;
	ListItem *l;
	int id;
    LinkedList *sockets;
    UnitTestLinkedList *socket;

	sockets = new LinkedList();

    if (argc > 1) {
        if (strcmp("STRESS", (char *)argv[1])==0)
            mode=1;
        }

    if (mode==0) { // basic example
        for (int i =0; i<10; ++i) {
            socket = new UnitTestLinkedList();
	        l = sockets->addItem(socket);
            }

       for (int i =0; i<10; ++i) {
 	        socket = (UnitTestLinkedList *)sockets->getItem(i);
	        if (socket!=NULL)
	            cout << "Element[" << i << "] = " << socket->getID() \n");
            }

       for (int i =9; i>0; i-=2) {
	        socket = (UnitTestLinkedList *)sockets->removeItem(i);
	        delete socket;
	        }

        for (int i =0; i<5; ++i) {
            socket = new UnitTestLinkedList();
	        l = sockets->addItem(socket);
            }

       for (int i =14; i>10; i-=2) {
	        socket = (UnitTestLinkedList *)sockets->removeItem(i);
	        delete socket;
	        }

        for (int i =0; i<5; ++i) {
            socket = new UnitTestLinkedList();
	        l = sockets->addItem(socket);
            }

	   // see what's left .....

	   cout << "Here's what's left" \n");

       for (int i =0; i<100; ++i) {
 	        socket = (UnitTestLinkedList *)sockets->getItem(i);
	        if (socket!=NULL)
	            cout << "Element[" << i << "] = " << socket->getID() \n");
            }
    } else { // STRESS TEST
        }

    safeDelete(sockets);
    }
