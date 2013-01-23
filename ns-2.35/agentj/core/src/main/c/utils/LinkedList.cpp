#include "LinkedList.h"

LinkedList::LinkedList(void) {
    head=NULL;
    tail=NULL;
    current=NULL;
	idCount=0;
}
 
LinkedList::~LinkedList(void) { 
    destroy(); 
}

ListItem* LinkedList::addItem(void *itemObject) {
    ListItem* n=NULL;

    n = new ListItem(itemObject, idCount);

	++idCount;

    if (head == NULL) {
        head = n;
		tail = n;
		current=n;
        head->setNext(NULL);
        head->setPrevious(NULL);
        return head;
    }
    else {
        n->setNext(NULL);
        n->setPrevious(NULL);
	 
        tail->setNext(n);
        n->setPrevious(tail);
        tail = n;
    }

    return n;
}

void LinkedList::destroy(void) {
    ListItem* n;
    ListItem* q;

    n = head;

    while (n != NULL) {
        q = n->getNext();
		// cout << "Linked List: deleting ListItem" \n");
        delete n;
        n = q;
        }
    head = NULL;
    tail = NULL;
}


/*
    Gets the ListItem for the given ID.
 */
ListItem* LinkedList::getListItem(int itemID) {
    ListItem* n;
    ListItem* q;
    bool searchOK;

    n = head;
	searchOK=true;

    if (n==NULL) {
		return NULL;
	}

    while (itemID != n->getID()) {
		if (n==tail) {
			searchOK=false;
			break;
		}

        q = n->getNext();
        n = q;
    }
  
	if (!searchOK) return NULL;

    return n;
    }

/*
    Gets the itemID and removes it from the list.
 */
void* LinkedList::getItem(int itemID) {
    ListItem* n;

    n = getListItem(itemID);

	if (n==NULL) return NULL;

    return n->getItem();
}


/*
    Gets the ListItem for the given object.
 */
ListItem* LinkedList::getListItem(void *itemObject) {
    ListItem* n;
    ListItem* q;
    bool searchOK;

//  	if (PAIEnvironment::getEnvironment()->isVerbose())
//		cout << "Linked List: getting object...." \n");

    n = head;
	searchOK=true;

    if (n==NULL) {
		return NULL;
	}

//  	if (PAIEnvironment::getEnvironment()->isVerbose())
//		cout << "Linked List: not null continuing...." \n");
    
    while (itemObject != n->getItem() ) {
		if (n==tail) {
			searchOK=false;
			break;
		}

        q = n->getNext();
        n = q;
    }
  
//  	if (PAIEnvironment::getEnvironment()->isVerbose())
//		cout << "Linked List: gettting Object -> search OK" << searchOK \n");

	if (!searchOK) return NULL;

    return n;
    }

/*
    Gets the itemObject from the list.
 */
void* LinkedList::getItem(void *itemObject) {
    ListItem* n;

    n = getListItem(itemObject);

	if (n==NULL) return NULL;

    return n->getItem();
}

bool LinkedList::contains(void *itemObject) {
    ListItem* n;
    ListItem* q;
    bool searchOK;

    n = head;
	searchOK=true;
    
    while (itemObject != n->getItem() ) {
		if (n==tail) {
			searchOK=false;
			break;
		}

        q = n->getNext();
        n = q;
    }
  
	if (!searchOK) return false;

    return true;
    }

/*
    Gets the item with the given itemID and removes the ListItem object from the
    list. It returns the actual object that was stored within the ListItem object.
    YOU MUST delete the actual objects stored in the list yourself as you cannot
    determine the type for safe deletion.
 */
void *LinkedList::removeItem(int itemID) {
    ListItem* n;
    ListItem* next;
    ListItem* previous;
    void *obj;

    n = getListItem(itemID);

	if (n==NULL) return NULL;

    next = n->getNext();
    previous = n->getPrevious();

    /* Take this one out of the list by changing the pointers of the
       next and previous packets in this list */

	if (next!=NULL)
		next->setPrevious(previous);
	else  { // this is the tail of the list so reset
		tail = previous; //set this whether it is null or not
		current = previous; // reset the current also
		}

	if (previous!=NULL)
	    previous->setNext(next);
	else { // this was the head of the list
		head = next;
		current = previous; // reset the current also
		}

    obj = n->getItem();

	delete n; // delete ListItem

    return obj;
    }

/*
    Gets the itemID and removes it from the list.
 */
void *LinkedList::removeItem(void *itemObject) {
    ListItem* n;
    ListItem* next;
    ListItem* previous;
    void *obj;

    n = getListItem(itemObject);

	if (n==NULL) return NULL;

    next = n->getNext();
    previous = n->getPrevious();

    /* Take this one out of the list by changing the pointers of the
       next and previous packets in this list */

	if (next!=NULL)
		next->setPrevious(previous);
	else {  // this is the tail of the list so reset
		tail = previous; //set this whether it is null or
		current = previous; // reset the current also
        }

		
	if (previous!=NULL)
		previous->setNext(next);
	else { // this was the head of the list
		head = next;
		current = previous; // reset the current also
		}

    obj = n->getItem();

	delete n;

    return obj;
    }
