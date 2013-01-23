/**
* @file protoRouteMgr.cpp
* 
* @brief Base class for providing a consistent interface to manage operating system (or other) routing engines 
*/
#include "protoRouteMgr.h"
#include "protoDebug.h"

bool ProtoRouteMgr::DeleteAllRoutes(ProtoAddress::Type addrType, unsigned int maxIterations)
{
    ProtoRouteTable rt;
    // Make multiple passes to get rid of possible
    // multiple routes to destinations
    // (TBD) extend ProtoRouteTable to support multiple routes per destination
    while (maxIterations-- > 0)
    {
        if (!GetAllRoutes(addrType, rt))
        {
            PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() error getting routes\n");
            return false;   
        }
        if (rt.IsEmpty()) break;
        if (!DeleteRoutes(rt))
        {
            PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() error deleting routes\n");
            return false;
        }
        rt.Destroy();
    }

    if (0 == maxIterations)
    {
        PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() couldn't seem to delete everything!\n");
        return false;
    }
    else
    {
        return true;
    }
}  // end ProtoRouteMgr::DeleteAllRoutes()

/**
 * Set direct (interface) routes and gateway routes.
 */
bool ProtoRouteMgr::SetRoutes(ProtoRouteTable& routeTable)
{
    bool result = true;
    ProtoRouteTable::Iterator iterator(routeTable);
    ProtoRouteTable::Entry* entry;
    // First, set direct (interface) routes 
    while ((entry = iterator.GetNextEntry()))
    {
        if (entry->IsDirectRoute())
        {
            if (!SetRoute(entry->GetDestination(),
                          entry->GetPrefixSize(),
                          entry->GetGateway(),
                          entry->GetInterfaceIndex(),
                          entry->GetMetric()))
            {
                PLOG(PL_ERROR, "ProtoRouteMgr::SetAllRoutes() failed to set direct route to: %s\n",
                        entry->GetDestination().GetHostString());
                result = false;   
            }
        }
    }
    // Second, set gateway routes 
    iterator.Reset();
    while ((entry = iterator.GetNextEntry()))
    {
        if (entry->IsGatewayRoute())
        {
            if (!SetRoute(entry->GetDestination(),
                          entry->GetPrefixSize(),
                          entry->GetGateway(),
                          entry->GetInterfaceIndex(),
                          entry->GetMetric()))
            {
                PLOG(PL_ERROR, "ProtoRouteMgr::SetAllRoutes() failed to set gateway route to: %s\n",
                        entry->GetDestination().GetHostString());
                result = false;   
            }
        }
    }
    
    return result;
}  // end ProtoRouteMgr::SetRoutes()

/**
 * Deletes gateway and direct (interface) routes and the
 * default entry if one exists.
 */
bool ProtoRouteMgr::DeleteRoutes(ProtoRouteTable& routeTable)
{
    bool result = true;
    ProtoRouteTable::Iterator iterator(routeTable);
    const ProtoRouteTable::Entry* entry;
    // First, delete gateway routes 
    while ((entry = iterator.GetNextEntry()))
    {
        if (entry->IsGatewayRoute())
        {
            if (!DeleteRoute(entry->GetDestination(),
			                 entry->GetPrefixSize(),
                             entry->GetGateway(),
                             entry->GetInterfaceIndex()))
            {
	            PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() failed to delete gateway route to: %s\n",
	                    entry->GetDestination().GetHostString());
	            result = false;   
            }
        }
    }
    // Second, delete direct (interface) routes
    iterator.Reset(); 
    while ((entry = iterator.GetNextEntry()))
    {
        if (entry->IsDirectRoute())
        {
            if (!DeleteRoute(entry->GetDestination(),
			                 entry->GetPrefixSize(),
                             entry->GetGateway(),
                             entry->GetInterfaceIndex()))
            {
	            PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() failed to delete direct route to: %s\n",
	                    entry->GetDestination().GetHostString());
	            result = false;   
            }
        }
    }
    // If there's a default entry delete it, too
    entry = routeTable.GetDefaultEntry();
    if (entry)
    {
        if (!DeleteRoute(entry->GetDestination(),
			             entry->GetPrefixSize(),
                         entry->GetGateway(),
                         entry->GetInterfaceIndex()))
        {
	        PLOG(PL_ERROR, "ProtoRouteMgr::DeleteAllRoutes() failed to delete default route\n");
	        result = false;   
        }
    }
    return result;
}  // end ProtoRouteMgr::DeleteRoutes()
