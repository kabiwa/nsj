#ifndef _LINKED_LIST
#define _LINKED_LIST

#include <iostream> 

/* ********************************************************************** */
/*                              LinkedList.h                                 */
/* ********************************************************************** */

/**
 * A list item for a linked list - could be used in other types of lists also
 * Note when you delete an item from a list this class deletes the item ...
 *
 * @TODO Change deleting item behaviour - not generic enough ....
 */
class ListItem {
public:

	/**
	 * Creates List Item from the given object and id
	 */
    ListItem(void *itemObject, int id_) { object=itemObject; id = id_;}

	/**
	 * Destroys the list Item BUT NOT THE OBJECT - you need to do this
	 * yourself as the destructors will not behave correctly if
	 * we try and delete the object, cast to a void * here .... 
	 */
	~ListItem(void) { 
	     }

	/**
	 * @returns the item that this ListItem object represents
	 */
    void *getItem() { return object; }

   	/**
	 * @returns the ID of this ListItem object 
	 */
    int getID() { 
//		if (PAIEnvironment::getEnvironment()->isVerbose())
//		cout << "LinkedList: ID is: " << id \n");
		return id; }

   	/**
	 * @returns the ID of this ListItem object 
	 */
    void setID(int theID) { id=theID; }

	/**
	 * @returns the next ListItem object in the Linked List
	 */
   ListItem *getNext() { return next; }

   /**
	 * @returns the previous ListItem object in the Linked List
	 */
   ListItem *getPrevious() { return previous; }

   /**
	 * Sets the next ListItem object in the Linked List to the
	 * given object reference
	 */
   void setNext(ListItem *item) { next=item; }

    /**
	 * Sets the previous ListItem object in the Linked List to the
	 * given object reference
	 */
   void setPrevious(ListItem *item) { previous=item; }

private:
	// internal mechanisms
    void *object;
    int id;
    ListItem* next;         // the next object
    ListItem* previous;         // not used at the moment
//    friend class LinkedList;  // so LinkedList can access next
};

/**
 * This is a generalized linked list class that can contain a hetergeneuos 
 * set of objects. It is modelled after the Java Vector class that provides
 * a shrinkable and expandable list for stroing a collection of objects. 
 * Items can be added delted and search for. Linked list can be used many
 * varied purposes. A Linked list contains a number of ListItem objects 
 * repesenting the individual objects.
 */
class LinkedList {

public:
	/**
	 * Creates an empty Linked List
	 */
    LinkedList(void);
	
	/**
	 * Destructor: deallocates all items in the list
	 */
    ~LinkedList(void);

	/**
	 * Adds the given item to the list
	 *
	 * @return the ListItem created for the object
	 */
    ListItem* addItem(void *itemObject);      // need to pass InputNodes to ListItem

	/**
	 * Gets the item with the specified ID from the list
	 *
	 * @return the item, if found, NULL otherwise
	 */
	void* getItem(int itemID);

	/**
	 * Gets the item with the specified ID from the list
	 *
	 * @return the item, if found, NULL otherwise
	 */
	ListItem* getListItem(int itemID);

	/**
	 * Gets the item from the list
	 *
	 * @return the item, if found, NULL otherwise
	 */
	void* getItem(void *itemObject);

	/**
	 * Gets the ListItem object from the list that contains the given object
	 *
	 * @return the item, if found, NULL otherwise
	 */
	ListItem* getListItem(void *itemObject);


	/**
	 * Tests if the specified object is a component in this list
	 *
	 * @return true if the given object is contained within the linked list,
	 * false otherwise
	 */
	bool contains(void *itemObject);

	/**
	 * Removes the item with the specified ID from the list. If
	 * successful, it returns the actual object stored within this ListObject, NULL
	 * otherwise.
	 *
	 * @return true if item was successfully removed
	 */
	void *removeItem(int itemID);

	/**
	 * Removes the ListItem object from the list that was used to store the object. If
	 * successful, it returns the actual object stored within this ListObject, NULL
	 * otherwise.
	 *
	 * @return true if item was successfully removed. Note that the object
	 * that the ListItem represents is 
	 * NOT deleted from the list - this has to be done manually because
	 * only the programmer knows what type of object it is
	 */
	void *removeItem(void *itemObject);

	void rewind() { current=head; }

    /**
	 * Gets the current ListItem Object from the list
	 */
	ListItem *getCurrentListItem() { return current; }

    /**
	 * Gets the next user object from the list
	 */
	ListItem *getNextListItem() { return iterate()? current : NULL; }

    /**
	 * Gets the current user Object from the list
	 */
	void *getCurrentItem() { if (current==NULL) return NULL; else current->getItem(); }

    /**
	 * Gets the next user Object from the list
	 */
	void *getNextItem() { return iterate()? current->getItem() : NULL; }

	/**
	 * Iterates to next item. Returns false if already at last item in list
	 */
	bool iterate() { if ((current==NULL) || (current==tail)) return false; else current = current->getNext(); return true; }

// destroys the list
    void destroy(void);
	
private:
// internal mechanisms
	int idCount;
    ListItem* head;
    ListItem* tail;
    ListItem* current;

}; 

#endif // _LINKED_LIST
