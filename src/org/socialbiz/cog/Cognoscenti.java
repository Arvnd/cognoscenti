package org.socialbiz.cog;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.socialbiz.cog.api.LightweightAuthServlet;
import org.socialbiz.cog.dms.FolderAccessHelper;
import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.exception.ProgramLogicError;
import org.socialbiz.cog.mail.EmailSender;
import org.socialbiz.cog.rest.ServerInitializer;

/**
 * This is the main class for the Cognoscenti object package.
 * This is a singleton pattern, and holds all the root configuration information.
 * Stores itself as an attribute on the servlet context
 * Use this class for initialization and accessing
 * the main index to objects within Cognoscenti
 *
 */
public class Cognoscenti {


    public ServerInitializer initializer = null;

    public Exception lastFailureMsg = null;
    public boolean isInitialized = false;
    public boolean initializingNow = false;
    //TODO: get rid of this static variable
    private static String serverId = "XXX";

    //hold on to the servlet context in case you need it later
    private ConfigFile theConfig;
    private File rootFolder;
    private UserCacheMgr userCacheMgr;

    //managing the known containers
    //TODO: get rid of this static variable
    private static List<NGPageIndex> allContainers;
    private static Hashtable<String, NGPageIndex> keyToContainer;
    private static Hashtable<String, NGPageIndex> upstreamToContainer;

    // there may be a number of pages that have unsent email, and so this is a
    // list of keys, but there can be extras in this list without problem
    public List<String> projectsWithEmailToSend = new ArrayList<String>();

    private Cognoscenti(ServletContext sc) {
        System.out.println("Cognoscenti Server Object == Constructing");
        rootFolder = new File(sc.getRealPath(""));
    }


    public static Cognoscenti getInstance(ServletContext sc) {
        Cognoscenti cog = (Cognoscenti) sc.getAttribute("cognoscenti");
        if (cog==null) {
            cog = new Cognoscenti(sc);
            sc.setAttribute("cognoscenti", cog);
        }
        return cog;
    }
    public static Cognoscenti getInstance(HttpSession session) {
        return getInstance(session.getServletContext());
    }
    public static Cognoscenti getInstance(HttpServletRequest req) {
        return getInstance(req.getSession());
    }


    /**
     * Starts the background task to initialize the server from config files.
     * Also attempts to initialize immediately.
     * This should be called ONLY ONCE when the server starts the first time.
     * After that, methods on this can pause and restart the server.
     * @param config the ServletConfig from the hosting TomCat server
     * @param resourceBundle the resource bundle from the wrapped servlet
     */
    public static void startTheServer(ServletConfig config) {
System.out.println("   _________                                                        __  .__  ");
System.out.println("   \\_   ___ \\  ____   ____   ____   ____  ______ ____  ____   _____/  |_|__| ");
System.out.println("   /    \\  \\/ /  _ \\ / ___\\ /    \\ /  _ \\/  ___// ___\\/ __ \\ /    \\   __\\  | ");
System.out.println("   \\     \\___(  <_> ) /_/  >   |  (  <_> )___ \\\\  \\__\\  ___/|   |  \\  | |  | ");
System.out.println("    \\______  /\\____/\\___  /|___|  /\\____/____  >\\___  >___  >___|  /__| |__| ");
System.out.println("           \\/      /_____/      \\/           \\/     \\/    \\/     \\/          ");
System.out.println("");
System.out.println("Cognoscenti Server Object == Start the Server");

        //first thing to do is to get the cognoscenti object associated with this app
        ServletContext sc = config.getServletContext();
        Cognoscenti cog = Cognoscenti.getInstance(sc);

        if (cog.initializer != null) {
            //report this, but otherwise ignore it.  Not bad enough to throw exception.
            System.out.println("Somthing wrong, the server is being initialized when it already has an initializer!");
        }

        cog.initializer = new ServerInitializer(cog);

        //call it directly to initialize (without waiting 30 seconds) if possible
        cog.initializer.run();
    }



    /**
     * For most of the server functions, this is the method to test to
     * see if the server is up, running, and handling requests.
     * When this is false, then only very special administrator requests
     * should be handled.
     */
    public boolean isRunning() {
        return (initializer!=null && initializer.serverInitState == ServerInitializer.STATE_RUNNING);
    }
    public boolean isPaused() {
        return (initializer!=null && initializer.serverInitState == ServerInitializer.STATE_PAUSED);
    }
    public boolean isFailed() {
        return (initializer!=null && initializer.serverInitState == ServerInitializer.STATE_FAILED);
    }
    public void pauseServer() {
        initializer.pauseServer();
    }
    public void resumeServer() {
        initializer.reinitServer();
    }
    public ConfigFile getConfig() {
        return theConfig;
    }
    public UserCacheMgr getUserCacheMgr() {
        return userCacheMgr;
    }

    //TODO: get rid of this static
    public static String getServerGlobalId() {
        return serverId;
    }


    /**
     * Call this in order to erase everything in memory and
     * free up all of the cached values.  Returns the module to
     * uninitialized state.  You  need to reinitialize after this.
     * Useful before calling garbage collect and reinitialize.
     */
    public synchronized void clearAllStaticVariables() {
        System.out.println("Cognoscenti Server Object == clear all static variables");
        NGPageIndex.clearAllStaticVars();
        NGBook.clearAllStaticVars();
        NGPage.clearAllStaticVars();
        NGTerm.clearAllStaticVars();
        SectionDef.clearAllStaticVars();
        UserManager.clearAllStaticVars();
        SiteReqFile.clearAllStaticVars();
        MicroProfileMgr.clearAllStaticVars();
        AuthDummy.clearStaticVariables();
        isInitialized = false;
        initializingNow = false;
        allContainers = null;
        keyToContainer = null;
        upstreamToContainer = null;
        projectsWithEmailToSend = null;
    }

    /**
     * From the passed in values will initialize the module.
     * @param rootFolder is the root on the installed folder and requires that there
     *        be a file at {rootFolder}/WEB-INF/config.txt
     * @param backgroundTimer is used for all the background activity for
     *        sending and receiving email, passing a null in will disable
     *        email sending and receiving
     * @exception will be thrown if anything in the configuration appears to be incorrect
     */
    public synchronized void initializeAll(Timer backgroundTimer) throws Exception {
        System.out.println("Cognoscenti Server Object == Initialize All");
        try {

            //TODO: reexamine this logic
            if (isInitialized) {
                //NOTE: two or more threads might try to call this at the same time.
                //Method is synchronized which will block threads, but when they get in here,
                //the first thing to do is check if you were initialized while blocked.
                if (rootFolder.equals(theConfig.getFileFromRoot(""))) {
                    //all OK, just return
                    return;
                }
                //was initialized to a different location, clean up that
                System.out.print("Reinitialization: changing from "+theConfig.getFileFromRoot("")
                        +" to "+rootFolder);
                clearAllStaticVariables();
            }

            initializingNow = true;
            theConfig = ConfigFile.initialize(rootFolder);
            theConfig.assertConfigureCorrectInternal();
            projectsWithEmailToSend = new ArrayList<String>();

            AuthDummy.initializeDummyRequest(this);
            UserManager.loadUpUserProfilesInMemory(this);
            userCacheMgr = new UserCacheMgr(this);

            NGPageIndex.initAllStaticVars();
            initIndexOfContainers();
            MicroProfileMgr.loadMicroProfilesInMemory(this);
            if (backgroundTimer!=null) {
                EmailSender.initSender(backgroundTimer, this);
                //SendEmailTimerTask.initEmailSender(backgroundTimer, this);
                EmailListener.initListener(backgroundTimer);
            }

            SiteReqFile.initSiteList(this);
            FolderAccessHelper.initLocalConnections(this);
            FolderAccessHelper.initCVSConnections(this);
            serverId = theConfig.getServerGlobalId();
            UserManager.init(this);
            LightweightAuthServlet.init(theConfig.getProperty("identityProvider"));

            isInitialized = true;
        }
        catch (Exception e) {
            lastFailureMsg = e;
            throw e;
        }
        finally {
            initializingNow = false;
        }
    }


    /**
     * This method must not be called by any method that is used during the
     * actual initialization itself. It is designed to be called by threads that
     * are NOT involved in initialization, in order to cleanly wait for initialization
     * to be completed.  This method will wait for up to 20 seconds
     * for initialization to complete with either a failure or a successful
     * initialization.
     */
    public boolean isInitialized() {
        // if the server is currently actively being initialized, then it if
        // probably worth
        // waiting a few seconds instead of failing
        int countDown = 40;
        while (initializingNow && --countDown > 0) {
            // test every 1/5 second, and wait up to 8 seconds for the
            // server to finish initializing, otherwise give up
            try {
                Thread.sleep(200);
            }
            catch (Exception e) {
                countDown = 0;
                // don't care what the exception is
                // just exit loop if sleep throws exception
            }
        }

        return (isInitialized);
    }
    /**
     * provides compatibility with earlier version that would attempt to
     * initialize on any given page refresh, and throw an exception if it
     * failed. Newer approach is to initialize once, but this will throw the
     * exception anyway if one was found attempting to initialize.
     */
    public void assertInitialized() throws Exception {
        if (!isInitialized()) {
            if (lastFailureMsg != null) {
                throw new NGException("nugen.exception.sys.not.initialize.correctly", null, lastFailureMsg);
            }
            throw new ProgramLogicError("NGPageIndex has never been initialized");
        }
    }


    public List<NGPageIndex> getAllContainers() {
        List<NGPageIndex> ret = new ArrayList<NGPageIndex>();
        if (allContainers == null) {
            return ret;
        }
        // if system is not initialized then return an empty vector
        for (NGPageIndex ngpi : allContainers) {
            if (!ngpi.isDeleted) {
                ret.add(ngpi);
            }
        }
        NGPageIndex.sortByName(ret);
        return ret;
    }

    public List<NGPageIndex> getDeletedContainers() {
        List<NGPageIndex> ret = new ArrayList<NGPageIndex>();
        for (NGPageIndex ngpi : getAllContainers()) {
            if (ngpi.isDeleted) {
                ret.add(ngpi);
            }
        }
        return ret;
    }



    public NGPageIndex getContainerIndexByKey(String key) throws Exception {
        assertInitialized();
        if (key == null) {
            // this programming mistake should never happen
            throw new ProgramLogicError("null value passed as key to getContainerIndexByKey");
        }
        return keyToContainer.get(key);
    }

    public NGPageIndex getContainerIndexByKeyOrFail(String key) throws Exception {
        NGPageIndex ngpi = getContainerIndexByKey(key);
        if (ngpi == null) {
            throw new NGException("nugen.exception.container.not.found", new Object[] { key });
        }
        return ngpi;
    }

    /**
     * Finding pages by name means that you might find more than one so you get
     * a vector back, which might be empty, it might have one or it might have
     * more pages.
     */
    public List<NGPageIndex> getPageIndexByName(String pageName) throws Exception {
        assertInitialized();

        NGTerm term = NGTerm.findTerm(pageName);
        if (term == null) {
            throw new NGException("nugen.exception.key.dont.have.alphanum", null);
        }
        return term.targetLeaves;
    }

    public boolean pageExists(String pageName) throws Exception {
        NGTerm term = NGTerm.findTermIfExists(pageName);
        if (term == null) {
            return false;
        }
        return (term.targetLeaves.size() > 0);
    }


    /**
     * This is a convenience function that looks a particular workspace
     * up in the index, finds the index entry, and then IF it is a
     * project, returns that with the right type (NGPage).
     * Fails if the key is not matched with anything, or if the key
     * is for a site.
     */
    public NGWorkspace getProjectByKeyOrFail(String key) throws Exception {
        NGPageIndex ngpi = getContainerIndexByKeyOrFail(key);
        NGContainer ngc = ngpi.getContainer();
        if (!(ngc instanceof NGWorkspace)) {
            throw new NGException("nugen.exception.container.not.project", new Object[] { key });
        }
        return (NGWorkspace) ngc;
    }

    /**
     * This is a convenience function that looks a particular site
     * up in the index, finds the index entry, and then IF it is a
     * site, returns that with the right type (NGBook)
     * Fails if the key is not matched with anything, or if the key
     * is for a project.
     */
    public NGBook getSiteByIdOrFail(String key) throws Exception {
        NGPageIndex ngpi = getContainerIndexByKeyOrFail(key);
        NGContainer ngc = ngpi.getContainer();
        if (!(ngc instanceof NGBook)) {
            throw new NGException("nugen.exception.container.not.account", new Object[] { key });
        }
        return (NGBook) ngc;
    }

    /**
     * This is for functions that operate the same on workspaces and sites.
     * (for example roles)
     * This is a convenience function that looks for either a workspace or
     * a site.  If the pageID is not '$' then a workspace with that key is returned.
     * If ifageID is '$', then the Site is returned with the siteId.
     */
    public NGContainer getWorkspaceOrSiteOrFail(String siteId, String pageId) throws Exception {
        if ("$".equals(pageId)) {
            NGPageIndex ngpi = getContainerIndexByKeyOrFail(siteId);
            NGContainer ngc = ngpi.getContainer();
            if (!(ngc instanceof NGBook)) {
                throw new NGException("nugen.exception.container.not.account", new Object[] { siteId });
            }
            return ngc;
        }
        else {
            NGPageIndex ngpi = getContainerIndexByKeyOrFail(pageId);
            NGContainer ngc = ngpi.getContainer();
            if (!(ngc instanceof NGWorkspace)) {
                throw new NGException("nugen.exception.container.not.project", new Object[] { pageId });
            }
            return ngc;
        }
    }


    /**
     * This is a convenience function that looks a particular site
     * up in the index, finds the index entry, and then IF it is a
     * site, returns that with the right type (NGBook)
     * Fails if the key is not matched with anything, or if the key
     * is for a project.
     */
    public NGBook getSiteById(String key) throws Exception {
        NGPageIndex ngpi = getContainerIndexByKey(key);
        if (ngpi==null) {
            return null;
        }
        NGContainer ngc = ngpi.getContainer();
        if (!(ngc instanceof NGBook)) {
            return null;
        }
        return (NGBook) ngc;
    }

    /**
     * Returns a vector of NGPageIndex objects which represent projects which
     * are all part of a single site. Should be called get all projects in site
     */
    public List<NGPageIndex> getAllProjectsInSite(String accountKey) throws Exception {
        List<NGPageIndex> ret = new ArrayList<NGPageIndex>();
        for (NGPageIndex ngpi : getAllContainers()) {
            if (ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PROJECT
                    && ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PAGE) {
                // only consider book/project style containers
                continue;
            }
            if (!accountKey.equals(ngpi.pageBookKey)) {
                // only consider if the project is in the site we look for
                continue;
            }
            if (ngpi.isDeleted) {
                // ignore deleted projects
                continue;
            }
            ret.add(ngpi);
        }
        return ret;
    }

    public List<NGPageIndex> getAllPagesForAdmin(UserProfile user) {
        List<NGPageIndex> ret = new ArrayList<NGPageIndex>();
        for (NGPageIndex ngpi : getAllContainers()) {
            if (ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PROJECT
                    && ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PAGE) {
                // only consider project style containers
                continue;
            }
            for (String admin : ngpi.admins) {
                if (user.hasAnyId(admin)) {
                    ret.add(ngpi);
                    break;
                }
            }
        }
        return ret;
    }

    public List<NGPageIndex> getProjectsUserIsPartOf(UserRef ale) throws Exception {
        List<NGPageIndex> ret = new ArrayList<NGPageIndex>();
        for (NGPageIndex ngpi : getAllContainers()) {
            if (ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PROJECT
                    && ngpi.containerType != NGPageIndex.CONTAINER_TYPE_PAGE) {
                // only consider project style containers
                continue;
            }
            NGContainer container = ngpi.getContainer();

            for (CustomRole role : container.getAllRoles()) {
                if (role.isPlayer(ale)) {
                    ret.add(ngpi);
                    break;
                }
            }
        }
        NGPageIndex.sortByName(ret);
        return ret;
    }

    public NGPage getProjectByUpstreamLink(String upstream) throws Exception {
        if (upstream==null || upstream.length()==0) {
            return null;
        }
        int lastSlash = upstream.lastIndexOf("/");
        if (lastSlash<10) {
            throw new Exception("upstream value was passed that does not look like a URL: "+upstream);
        }
        NGPageIndex ngpi = upstreamToContainer.get(upstream.substring(0,lastSlash+1));
        if (ngpi != null) {
            return ngpi.getPage();
        }
        return null;
    }



    private synchronized void initIndexOfContainers() throws Exception {
        if (allContainers == null) {
            scanAllPages();
        }
    }

    private synchronized void scanAllPages() throws Exception {
        System.out.println("Beginning SCAN for all pages in system");
        List<File> allPageFiles = new ArrayList<File>();
        List<File> allProjectFiles = new ArrayList<File>();
        NGTerm.initialize();
        keyToContainer = new Hashtable<String, NGPageIndex>();
        upstreamToContainer = new Hashtable<String, NGPageIndex>();
        allContainers = new ArrayList<NGPageIndex>();

        //TODO: eliminate statics, put them as members of this Cognoscenti class!
        NGBook.initStaticVars();

        List<File> allSiteFiles = new ArrayList<File>();
        for (File libDirectory : theConfig.getSiteFolders()) {
            seekProjectsAndSites(libDirectory, allProjectFiles, allSiteFiles);
        }

        // now process the site files if any
        for (File aSitePath : allSiteFiles) {
            try {
                NGBook ngb = NGBook.readSiteAbsolutePath(aSitePath);
                NGBook.registerSite(ngb);
                makeIndex(ngb);
            }
            catch (Exception eig) {
                reportUnparseableFile(aSitePath, eig);
            }
        }
        // page files for data folder
        for (File aProjPath : allPageFiles) {
            try {
                NGWorkspace aPage = NGWorkspace.readWorkspaceAbsolutePath(aProjPath);
                makeIndex(aPage);
            }
            catch (Exception eig) {
                reportUnparseableFile(aProjPath, eig);
            }
        }
        // now process the project files if any
        for (File aProjPath : allProjectFiles) {
            try {
                NGWorkspace aProj = NGWorkspace.readWorkspaceAbsolutePath(aProjPath);
                makeIndex(aProj);
            }
            catch (Exception eig) {
                reportUnparseableFile(aProjPath, eig);
            }
        }
        System.out.println("Concluded SCAN for all pages in system.");

    }

    private void reportUnparseableFile(File badFile, Exception eig) {
        AuthRequest dummy = AuthDummy.serverBackgroundRequest();
        Exception wrapper = new Exception("Failure reading file during Initialization: "
                + badFile.toString(), eig);
        dummy.logException("Initialization Loop Continuing After Failure", wrapper);
    }

    private void seekProjectsAndSites(File folder, List<File> pjs, List<File> acts)
            throws Exception {

        // only use the first ".sp" file or ".site" file in a given folder
        boolean foundOne = false;

        File cogFolder = new File(folder, ".cog");
        if (cogFolder.exists()) {
            File projectFile = new File(cogFolder, "ProjInfo.xml");
            if (projectFile.exists()) {
                pjs.add(projectFile);
                foundOne = true;
            }
            File siteFile = new File(cogFolder, "SiteInfo.xml");
            if (siteFile.exists()) {
                acts.add(siteFile);
                foundOne = true;
            }
        }

        for (File child : folder.listFiles()) {
            String name = child.getName();
            if (child.isDirectory()) {
                // only drill down if not the cog folder
                if (!name.equalsIgnoreCase(".cog")) {
                    seekProjectsAndSites(child, pjs, acts);
                }
                continue;
            }
            if (foundOne) {
                // ignore all files after one is found
                continue;
            }
            if (name.endsWith(".sp")) {
                // this is the migration case, a .sp file exists, but the
                // .cog/ProjInfo.xml
                // does not exist, so move the file there immediately.
                if (!cogFolder.exists()) {
                    cogFolder.mkdirs();
                }
                String key = name.substring(0, name.length() - 3);
                File keyFile = new File(cogFolder, "key_" + key);
                keyFile.createNewFile();
                File projInfoFile = new File(cogFolder, "ProjInfo.xml");
                UtilityMethods.copyFileContents(child, projInfoFile);
                child.delete();
                pjs.add(projInfoFile);
                foundOne = true;
            }
            else if (name.endsWith(".site")) {
                acts.add(child);
                foundOne = true;
            }
        }
    }

    public void makeIndex(NGContainer ngc) throws Exception {
        String key = ngc.getKey();

        // clean up old index entries using old name
        NGPageIndex foundPage = keyToContainer.get(key);
        if (foundPage != null) {
            foundPage.unlinkAll();
            allContainers.remove(foundPage);
            keyToContainer.remove(foundPage.containerKey);
        }

        NGPageIndex bIndex = new NGPageIndex(ngc);
        if (bIndex.containerType == 0) {
            throw new Exception("uninitialized ngpi.containerType in makeIndex");
        }
        allContainers.add(bIndex);
        keyToContainer.put(key, bIndex);

        if (ngc instanceof NGPage) {
            String upstream = ((NGPage)ngc).getUpstreamLink();
            if (upstream!=null && upstream.length()>0) {
                int lastSlash = upstream.lastIndexOf("/");
                upstreamToContainer.put(upstream.substring(0,lastSlash+1), bIndex);
            }
        }



        // look for email and remember if there is some
        if (ngc instanceof NGPage) {
            if (((NGPage) ngc).countEmailToSend() > 0) {
                projectsWithEmailToSend.add(key);
            }
        }
    }

    /**
     * Get the first page that has email that still needs to be sent Returns
     * null if there are not any
     */
    public String getPageWithEmailToSend() throws Exception {
        if (projectsWithEmailToSend.size() == 0) {
            return null;
        }
        return projectsWithEmailToSend.get(0);
    }

    /**
     * When you find that a project does not have any more email to send,
     * call removePageFromEmailToSend to remove from the cached list.
     */
    public void removePageFromEmailToSend(String key) {
        projectsWithEmailToSend.remove(key);
    }

}
