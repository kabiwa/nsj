#include "protoNet.h"
#include "protoDebug.h"

#include <unistd.h>
#include <stdlib.h>  // for atoi()
/*
#ifdef HAVE_IPV6
#ifdef MACOSX
#include <arpa/nameser.h>
#endif // MACOSX
#include <resolv.h>
#endif  // HAVE_IPV6
*/
#include <sys/ioctl.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <errno.h>
#include <fcntl.h>

#ifndef SIOCGIFHWADDR
#if defined(SOLARIS) || defined(IRIX)
#include <sys/sockio.h> // for SIOCGIFADDR ioctl
#include <netdb.h>      // for rest_init()
#include <sys/dlpi.h>
#include <stropts.h>
#else
#include <net/if.h>
#include <net/if_dl.h>
#include <net/if_types.h>
#include <ifaddrs.h> 
#endif  // if/else (SOLARIS || IRIX)
#endif  // !SIOCGIFHWADDR

// Implementation of ProtoNet functions for Unix systems.  These are the mechanisms that are common to
// most Unix systems.  Some additional ProtoNet mechanisms that have Linux- or MacOS-specific code are
// implemented in "src/linux/linuxNet.cpp" or "src/macos/macosNet.cpp", etc.

#ifndef HAVE_IPV6
// Interval helper function for older (non-IPv6) systems,
// returns number of interfaces, interface records in "struct ifconf"
// conf.ifc_buf must be deleted by calling program when done
// note that use of SIOCGIFCONF is deprecated
static int GetInterfaceList(struct ifconf& conf)
{
    int sockFd = socket(PF_INET, SOCK_DGRAM, 0);
    if (sockFd < 0) 
    {
        PLOG(PL_ERROR, "ProtoNet::GetInterfaceList() socket() error: %s\n", GetErrorString());
        return 0;
    }

    int ifNum = 32;  // first guess for max # of interfaces
#ifdef SIOCGIFNUM  // Solaris has this, others might
	if (ioctl(sock, SIOCGIFNUM, &ifNum) >= 0) ifNum++;
#endif // SIOCGIFNUM
    // We loop here until we get a fully successful SIOGIFCONF
    // This returns us a list of all interfaces
    int bufLen;
    conf.ifc_buf = NULL;
    do
    {
        if (NULL != conf.ifc_buf) delete[] conf.ifc_buf;  // remove previous buffer
        bufLen = ifNum * sizeof(struct ifreq);
        conf.ifc_len = bufLen;
        conf.ifc_buf = new char[bufLen];
        if ((NULL == conf.ifc_buf))
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceList() new conf.ifc_buf error: %s\n", GetErrorString());
            conf.ifc_len = 0;
            break;
        }
        if ((ioctl(sockFd, SIOCGIFCONF, &conf) < 0))
        {
            PLOG(PL_WARN, "ProtoNet::GetInterfaceList() ioctl(SIOCGIFCONF) warning: %s\n", GetErrorString());
			conf.ifc_len = 0;  // reset for fall-through below
        }
        ifNum *= 2;  // last guess not big enough, so make bigger & try again
    } while (conf.ifc_len >= bufLen);
    close(sockFd);  // done with socket (whether error or not)
    return (conf.ifc_len / sizeof(struct ifreq));  // number of interfaces (or 0)

// above follows Stevens book & may be the most general for all *nix platforms
// below is simpler, works (at least) on Fedora Linux
// found it on http://codingrelic.geekhold.com/
//
//	conf.ifc_len = 0;
//	conf.ifc_buf = NULL;
//	int sockFd;
//
//	if (((sockFd = socket(PF_INET, SOCK_DGRAM, 0)) < 0) ||  // get socket
//	    (ioctl(sockFd, SIOCGIFCONF, &conf) < 0) ||  // get # of records
//	    ((conf.ifc_buf = new char[conf.ifc_len]) == NULL) ||  // make if bfr
//	    (ioctl(sockFd, SIOCGIFCONF, &conf) < 0))  // fill bfr with if records
//	{
//		PLOG(PL_ERROR, "ProtoNet::GetInterfaceList() error: %s\n",
//		               GetErrorString());
//		conf.ifc_len = 0;  // reset for below
//	}
//
//	close(sockFd);  // done with socket, error or no
//
//	return (conf.ifc_len / sizeof(struct ifreq));  // # records (or 0)
}  // end ProtoNet::GetInterfaceList()
#endif // !HAVE_IPV6

bool ProtoNet::GetInterfaceAddressList(const char*         interfaceName,
                                       ProtoAddress::Type  addressType,
                                       ProtoAddressList&   addrList,
                                       unsigned int*       interfaceIndex)
{
    struct ifreq req;
    memset(&req, 0, sizeof(struct ifreq));
    strncpy(req.ifr_name, interfaceName, IFNAMSIZ);
    int socketFd = -1;
    switch (addressType)
    {
        case ProtoAddress::IPv4:
            req.ifr_addr.sa_family = AF_INET;
            socketFd = socket(PF_INET, SOCK_DGRAM, 0);
            break;
#ifdef HAVE_IPV6
        case ProtoAddress::IPv6:
            req.ifr_addr.sa_family = AF_INET6;
            socketFd = socket(PF_INET6, SOCK_DGRAM, 0);
            break;
#endif // HAVE_IPV6
        default:
            req.ifr_addr.sa_family = AF_UNSPEC;
            socketFd = socket(PF_INET, SOCK_DGRAM, 0);
            break;
    }
    
    if (socketFd < 0)
    {
        PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() socket() error: %s\n", GetErrorString()); 
        return false;
    }   
    
    if (ProtoAddress::ETH == addressType)
    {
#ifdef SIOCGIFHWADDR
        // Probably Linux
        // Get hardware (MAC) address instead of IP address
        if (ioctl(socketFd, SIOCGIFHWADDR, &req) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() ioctl(SIOCGIFHWADDR) error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;   
        }  
        else
        {
            close(socketFd);
            if (NULL != interfaceIndex) *interfaceIndex = req.ifr_ifindex;
            ProtoAddress ethAddr;
            if (!ethAddr.SetRawHostAddress(ProtoAddress::ETH,
                                           (const char*)&req.ifr_hwaddr.sa_data,
                                           IFHWADDRLEN))
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: invalid ETH addr?\n");
                return false;
            }   
            if (!addrList.Insert(ethAddr))
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: unable to add ETH addr to list.\n");
                return false;
            }
            return true;            
        }
#else
#if defined(SOLARIS) || defined(IRIX)
        // Use DLPI instead
        close(socketFd);
        char deviceName[32];
        snprintf(deviceName, 32, "/dev/%s", interfaceName);
        char* ptr = strpbrk(deviceName, "0123456789");
        if (NULL == ptr)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() invalid interface\n");
            return false;
        }
        int ifNumber = atoi(ptr);
        *ptr = '\0';    
        if ((socketFd = open(deviceName, O_RDWR)) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi open() error: %s\n",
                    GetErrorString());
            return false;
        }
        // dlp device opened, now bind to specific interface
        UINT32 buffer[8192];
        union DL_primitives* dlp = (union DL_primitives*)buffer;
        dlp->info_req.dl_primitive = DL_INFO_REQ;
        struct strbuf msg;
        msg.maxlen = 0;
        msg.len = DL_INFO_REQ_SIZE;
        msg.buf = (caddr_t)dlp;
        if (putmsg(socketFd, &msg, NULL, RS_HIPRI) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi putmsg(1) error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;
        }
        msg.maxlen = 8192;
        msg.len = 0;
        int flags = 0;
        if (getmsg(socketFd, &msg, NULL, &flags) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(1) error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;
        }
        if ((DL_INFO_ACK != dlp->dl_primitive) ||
            (msg.len <  (int)DL_INFO_ACK_SIZE))
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(1) error: unexpected response\n");
            close(socketFd);
            return false;
        }
        if (DL_STYLE2 == dlp->info_ack.dl_provider_style)
        {
            dlp->attach_req.dl_primitive = DL_ATTACH_REQ;
            dlp->attach_req.dl_ppa = ifNumber;
            msg.maxlen = 0;
            msg.len = DL_ATTACH_REQ_SIZE;
            msg.buf = (caddr_t)dlp;
            if (putmsg(socketFd, &msg, NULL, RS_HIPRI) < 0)
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi putmsg(DL_ATTACH_REQ) error: %s\n",
                        GetErrorString());
                close(socketFd);
                return false;
            }
            msg.maxlen = 8192;
            msg.len = 0;
            flags = 0;
            if (getmsg(socketFd, &msg, NULL, &flags) < 0)
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(DL_OK_ACK) error: %s\n",
                        GetErrorString());
                close(socketFd);
                return false;
            }
            if ((DL_OK_ACK != dlp->dl_primitive) ||
                (msg.len <  (int)DL_OK_ACK_SIZE))
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(DL_OK_ACK) error: unexpected response\n");
                close(socketFd);
                return false;
            }
        }
        memset(&dlp->bind_req, 0, DL_BIND_REQ_SIZE);
        dlp->bind_req.dl_primitive = DL_BIND_REQ;
#ifdef DL_HP_RAWDLS
	    dlp->bind_req.dl_sap = 24;	
	    dlp->bind_req.dl_service_mode = DL_HP_RAWDLS;
#else
	    dlp->bind_req.dl_sap = DL_ETHER;
	    dlp->bind_req.dl_service_mode = DL_CLDLS;
#endif
        msg.maxlen = 0;
        msg.len = DL_BIND_REQ_SIZE;
        msg.buf = (caddr_t)dlp;
        if (putmsg(socketFd, &msg, NULL, RS_HIPRI) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi putmsg(DL_BIND_REQ) error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;     
        }
        msg.maxlen = 8192;
        msg.len = 0;
        flags = 0;
        if (getmsg(socketFd, &msg, NULL, &flags) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(DL_BIND_ACK) error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;
        }
        if ((DL_BIND_ACK != dlp->dl_primitive) ||
            (msg.len <  (int)DL_BIND_ACK_SIZE))
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg(DL_BIND_ACK) error: unexpected response\n");
            close(socketFd);
            return false;
        }
        // We're bound to the interface, now request interface address
        dlp->info_req.dl_primitive = DL_INFO_REQ;
        msg.maxlen = 0;
        msg.len = DL_INFO_REQ_SIZE;
        msg.buf = (caddr_t)dlp;
        if (putmsg(socketFd, &msg, NULL, RS_HIPRI) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi putmsg() error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;     
        }
        msg.maxlen = 8192;
        msg.len = 0;
        flags = 0;
        if (getmsg(socketFd, &msg, NULL, &flags) < 0)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg() error: %s\n",
                    GetErrorString());
            close(socketFd);
            return false;
        }
        if ((DL_INFO_ACK != dlp->dl_primitive) || (msg.len <  (int)DL_INFO_ACK_SIZE))
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() dlpi getmsg() error: unexpected response\n");
            close(socketFd);
            return false;
        }
        ProtoAddress macAddr;
        macAddr.SetRawHostAddress(addressType, (char*)(buffer + dlp->physaddr_ack.dl_addr_offset), 6);
        if (NULL != interfaceIndex) *interfaceIndex = ifNumber;
        close(socketFd);
        if (!addrList.Insert(macAddr))
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: unable to add ETH addr to list.\n");
            return false; 
        }
        return true;
#else
        // For now, assume we're BSD and use getifaddrs()
        close(socketFd);  // don't need the socket
        struct ifaddrs* ifap;
        if (0 == getifaddrs(&ifap))
        {
            // TBD - Look for AF_LINK address for given "interfaceName"
            struct ifaddrs* ptr = ifap;
            while (ptr)
            {
                if (ptr->ifa_addr && (AF_LINK == ptr->ifa_addr->sa_family))
                {
                    if (!strcmp(interfaceName, ptr->ifa_name))
                    {
                        // (TBD) should we confirm sdl->sdl_type == IFT_ETHER?
                        struct sockaddr_dl* sdl = (struct sockaddr_dl*)ptr->ifa_addr;
                        if (IFT_ETHER != sdl->sdl_type)
                        {
                            freeifaddrs(ifap);
                            PLOG(PL_WARN, "ProtoNet::GetInterfaceAddressList() error: non-Ethertype iface: %s\n", interfaceName);
                            return false;
                        }
                        ProtoAddress macAddr;
                        macAddr.SetRawHostAddress(addressType, 
                                                  sdl->sdl_data + sdl->sdl_nlen,
                                                  sdl->sdl_alen);
                        if (NULL != interfaceIndex) 
                            *interfaceIndex = sdl->sdl_index;
                        freeifaddrs(ifap);
                        if (!addrList.Insert(macAddr))
                        {
                            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: unable to add ETH addr to list.\n");
                            return false; 
                        }
                        return true;
                    }
                }   
                ptr = ptr->ifa_next;
            }
            freeifaddrs(ifap);
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() unknown interface name\n");
            return false; // change to true when implemented
        }
        else
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() getifaddrs() error: %s\n",
                    GetErrorString());
            return false;  
        }
#endif // if/else (SOLARIS || IRIX)
#endif // if/else SIOCGIFHWADDR
    }  // end if (ETH == addrType)
    if (ioctl(socketFd, SIOCGIFADDR, &req) < 0)
    {
        // (TBD - more sophisticated warning logic here
        PLOG(PL_DEBUG, "ProtoNet::GetInterfaceAddressList() ioctl(SIOCGIFADDR) error for iface>%s: %s\n",
                      interfaceName, GetErrorString()); 
        close(socketFd);
        // Perhaps "interfaceName" is an address string?
        ProtoAddress ifAddr;
        if (ifAddr.ConvertFromString(interfaceName))
        //if (ifAddr.ResolveFromString(interfaceName))
        {
            char nameBuffer[IFNAMSIZ+1];
            if (GetInterfaceName(ifAddr, nameBuffer, IFNAMSIZ+1))
            {
                return GetInterfaceAddressList(nameBuffer, addressType, addrList, interfaceIndex);
            }
            else
            {
                PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: unknown interface address\n");
                return false;
            }
        }
        
        return false; 
    }
    close(socketFd);
    
    if (NULL != interfaceIndex) 
        *interfaceIndex = GetInterfaceIndex(req.ifr_name);
    
    ProtoAddress ifAddr;
#ifdef MACOSX
    // (TBD) make this more general somehow???
    if (0 == req.ifr_addr.sa_len)
    {
        PLOG(PL_DEBUG, "ProtoNet::GetInterfaceAddressList() warning: no addresses for given family?\n");
        return false;
    }
    else 
#endif // MACOSX
    if (ifAddr.SetSockAddr(req.ifr_addr))
    {
        if (addrList.Insert(ifAddr))
        {
            return true;
        }
        else
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: unable to add ifAddr to list\n");
            return false;
        }
    }
    else
    {
        PLOG(PL_ERROR, "ProtoNet::GetInterfaceAddressList() error: invalid address family\n");
        return false;
    }
}  // end ProtoNet::GetInterfaceAddressList()

unsigned int ProtoNet::GetInterfaceIndex(const char* interfaceName)
{
    unsigned int index = 0;    
#ifdef HAVE_IPV6
    index = if_nametoindex(interfaceName);
#else
#ifdef SIOCGIFINDEX
    int sockFd = socket(PF_INET, SOCK_DGRAM, 0);
    if (sockFd < 0) 
    {
        PLOG(PL_WARN, "ProtoNet::GetInterfaceIndex() socket() error: %s\n",
                       GetErrorString());
        return 0;
    }
    struct ifreq req;
    strncpy(req.ifr_name, interfaceName, IFNAMSIZ);
    if (ioctl(sockFd, SIOCGIFINDEX, &req) < 0)
        PLOG(PL_WARN, "ProtoNet::GetInterfaceIndex() ioctl(SIOCGIFINDEX) error: %s\n",
                       GetErrorString());
    else
        index =  req.ifr_ifindex;
    close(sockFd);
#else
    PLOG(PL_ERROR, "ProtoNet::GetInterfaceIndex() error: interface indices not supported\n");
    return 0;
#endif  // if/else SIOCGIFINDEX
#endif  // if/else HAVE_IPV6
    if (0 == index)
    {
        // Perhaps "interfaceName" was an address string?
        ProtoAddress ifAddr;
        if (ifAddr.ResolveFromString(interfaceName))
        {
            char nameBuffer[IFNAMSIZ+1];
            if (GetInterfaceName(ifAddr, nameBuffer, IFNAMSIZ+1))
                return GetInterfaceIndex(nameBuffer);
        }
    }
    return index;
}  // end ProtoNet::GetInterfaceIndex()

unsigned int ProtoNet::GetInterfaceIndices(unsigned int* indexArray, unsigned int indexArraySize)
{
    unsigned int indexCount = 0;
#ifdef HAVE_IPV6
    struct if_nameindex* ifdx = if_nameindex();
    if (NULL == ifdx) return 0;  // no interfaces found
    struct if_nameindex* ifPtr = ifdx;
	while ((0 != ifPtr->if_index))
	{
		// need to take into account (NULL, 0) input (see GetInterfaceName)
		if ((NULL != indexArray) && (indexCount < indexArraySize))
			indexArray[indexCount] = ifPtr->if_index;
		indexCount++;         
		ifPtr++;
	} 
	if_freenameindex(ifdx);
#else  // !HAVE_IPV6
#ifdef SIOCGIFINDEX
    struct ifconf conf;
    conf.ifc_buf = NULL;  // initialize
	indexCount = GetInterfaceList(conf);
    if (NULL != indexArray)
    {
        if (indexCount < indexArraySize) indexArraySize = indexCount;
        for (unsigned int i = 0; i < indexArraySize; i++)
            indexArray[i] = GetInterfaceIndex(conf.ifc_req[i].ifr_name);
    }
    if (NULL != conf.ifc_buf) delete[] conf.ifc_buf;

#else  // !SIOCGIFINDEX
	PLOG(PL_ERROR, "ProtoNet::GetInterfaceIndices() error: interface indices not supported\n");
#endif  // if/else SIOCGIFINDEX
#endif  // if/else HAVE_IPV6
    return indexCount;
}  // end ProtoNet::GetInterfaceIndices()

// given addrType, searches through interface list, returns first non-loopback address found
// TBD - should we _try_ to find a non-link-local addr as well?
bool ProtoNet::FindLocalAddress(ProtoAddress::Type addrType, ProtoAddress& theAddress)
{
	bool foundLocal = false;  // default
#ifdef HAVE_IPV6
	struct if_nameindex* ifdx = if_nameindex();
    if (NULL == ifdx) return false;
	struct if_nameindex* ifPtr = ifdx;  // first interface

	while (0 != ifPtr->if_index)
	{
		if (GetInterfaceAddress(ifPtr->if_name, addrType, theAddress))
		{
            // Should we _try_ to find a non-link-local addr?
            if (!theAddress.IsLoopback())
			{
				foundLocal = true;      
				break;
			}
		}            
		ifPtr++;
	}
	if_freenameindex(ifdx);
#else  // !HAVE_IPV6
	struct ifconf conf;
	conf.ifc_buf = NULL;  // initialize
	int ifCount = GetInterfaceList(conf);
    for (int i = 0; i < ifCount; i++)
	{
		if (GetInterfaceAddress(conf.ifc_req[i].ifr_name, addrType, theAddress))
		{
			if (!theAddress.IsLoopback())
			{
                // Should we _try_ to find a non-link-local addr?
				foundLocal = true;
				break;
			}
		}
	}
	delete[] conf.ifc_buf;
#endif // if/else HAVE_IPV6

	// (TBD) set loopback addr if nothing else?
	if (!foundLocal)
		PLOG(PL_WARN, "ProtoNet::FindLocalAddress() no %s addr assigned\n",
		              (ProtoAddress::IPv6 == addrType) ? "IPv6" : "IPv4");
	return foundLocal;
}  // end ProtoNet::FindLocalAddress()

bool ProtoNet::GetInterfaceName(unsigned int index, char* buffer, unsigned int buflen)
{
#ifdef HAVE_IPV6
    char ifName[IFNAMSIZ+1];
    if (NULL != if_indextoname(index, ifName))
    {
        strncpy(buffer, ifName, buflen);
        return true;
    }
    else
    {
        return false;
    }
#else
#ifdef SIOCGIFNAME
    int sockFd = socket(PF_INET, SOCK_DGRAM, 0);
    if (sockFd < 0) 
    {
        PLOG(PL_ERROR, "ProtoNet::GetInterfaceName() socket() error: %s\n",
                       GetErrorString());
        return false;
    }
    struct ifreq req;
    req.ifr_ifindex = index;
    if (ioctl(sockFd, SIOCGIFNAME, &req) < 0)
    {
        PLOG(PL_ERROR, "ProtoNet::GetInterfaceName() ioctl(SIOCGIFNAME) error: %s\n",
                       GetErrorString());
        close(sockFd);
        return false;
    }
    close(sockFd);
    if (buflen > IFNAMSIZ)
    {
        buffer[IFNAMSIZ] = '\0';
        buflen = IFNAMSIZ;
    }
    strncpy(buffer, req.ifr_name, buflen);
    return true;
#else
    PLOG(PL_ERROR, "ProtoNet::GetInterfaceName() error: getting name by index not supported\n");
    return false;
#endif // if/else SIOCGIFNAME
#endif // if/else HAVE_IPV6
}  // end ProtoNet::GetInterfaceName(by index)

bool ProtoNet::GetInterfaceName(const ProtoAddress& ifAddr, char* buffer, unsigned int buflen)
{      
#ifdef HAVE_IPV6
    // Go through list, looking for matching address
    struct if_nameindex* ifdx = if_nameindex();
    struct if_nameindex* ptr = ifdx;
    if (NULL == ifdx) return false;   // no interfaces?
    while (0 != ptr->if_index)
    {
        ProtoAddressList addrList;
        if (GetInterfaceAddressList(ptr->if_name, ifAddr.GetType(), addrList))
        {
            ProtoAddressList::Iterator iterator(addrList);
            ProtoAddress theAddress;
            while (iterator.GetNextAddress(theAddress))
            {
                if (ifAddr.HostIsEqual(theAddress))
                {
                    if (buflen > IFNAMSIZ)
                    {
                        buffer[IFNAMSIZ] = '\0';
                        buflen = IFNAMSIZ;
                    }
                    strncpy(buffer, ptr->if_name, buflen);
                    if_freenameindex(ifdx);
                    return true;;
                }   
            }
        }
        ptr++;
    }
    if_freenameindex(ifdx);
#else
    // First, find out how many interfaces there are
    unsigned int indexCount = GetInterfaceIndices(NULL, 0);
    if (indexCount > 0)
    {
        unsigned int* indexArray = new unsigned int[indexCount];
        if (NULL == indexArray)
        {
            PLOG(PL_ERROR, "ProtoNet::GetInterfaceName() new indexArray error: %S\n",
                    GetErrorString());
            return false;
        }
        indexCount = GetInterfaceIndices(indexArray, indexCount);
        for (unsigned int i = 0; i < indexCount; i++)
        {
            char ifName[IFNAMSIZ+1];
            ifName[IFNAMSIZ] = '\0';
            if (GetInterfaceName(indexArray[i], ifName, IFNAMSIZ))
            {
                ProtoAddressList addrList;
                if (GetInterfaceAddressList(ifName, ifAddr.GetType(), addrList))
                {
                    ProtoAddressList::Iterator iterator(addrList);
                    ProtoAddress theAddress;
                    while (iterator.GetNextAddress(theAddress))
                    {
                        if (ifAddr.HostIsEqual(theAddress))
                        {
                            if (buflen > IFNAMSIZ)
                            {
                                buffer[IFNAMSIZ] = '\0';
                                buflen = IFNAMSIZ;
                            }
                            strncpy(buffer, ifName, buflen);
                            delete[] indexArray;
                            return true;;
                        }   
                    }
                }
            }
            else
            {
                PLOG(PL_WARN, "ProtoNet::GetInterfaceName() warning: GetInterfaceName(%d) failure\n",
                              indexArray[i]);
            }
        }
        delete[] indexArray;
    }
#endif // if/else HAVE_IPV6
    return false;
}  // end  ProtoNet::GetInterfaceName(by address)


