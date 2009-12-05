/* ******************************************************************************
 * Copyright (c) 2006-2009 XMind Ltd. and others.
 * 
 * This file is a part of XMind 3. XMind releases 3 and
 * above are dual-licensed under the Eclipse Public License (EPL),
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 * and the GNU Lesser General Public License (LGPL), 
 * which is available at http://www.gnu.org/licenses/lgpl.html
 * See http://www.xmind.net/license.html for details.
 * 
 * Contributors:
 *     XMind Ltd. - initial API and implementation
 *******************************************************************************/
package org.xmind.core.internal.dom;

import static org.xmind.core.internal.dom.DOMConstants.ATTR_FULL_PATH;
import static org.xmind.core.internal.dom.DOMConstants.ATTR_MEDIA_TYPE;
import static org.xmind.core.internal.dom.DOMConstants.TAG_ENCRYPTION_DATA;
import static org.xmind.core.internal.dom.InternalDOMUtils.getParentPath;
import static org.xmind.core.internal.dom.InternalDOMUtils.isParentPath;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmind.core.Core;
import org.xmind.core.CoreException;
import org.xmind.core.IEncryptionData;
import org.xmind.core.IFileEntry;
import org.xmind.core.IManifest;
import org.xmind.core.IWorkbook;
import org.xmind.core.internal.FileEntry;
import org.xmind.core.internal.security.Crypto;
import org.xmind.core.io.IInputSource;
import org.xmind.core.io.IStorage;
import org.xmind.core.util.DOMUtils;

public class FileEntryImpl extends FileEntry {

    private static final List<IFileEntry> NO_SUB_FILE_ENTRIES = Collections
            .emptyList();

    private Element implementation;

    private ManifestImpl ownedManifest;

    private Integer refCount = null;

    private EncryptionDataImpl encData;

    private boolean ignoreEncryption = false;

    public FileEntryImpl(Element implementation, ManifestImpl ownedManifest) {
        this.implementation = implementation;
        this.ownedManifest = ownedManifest;
    }

    public Element getImplementation() {
        return implementation;
    }

    public String getMediaType() {
        return implementation.getAttribute(ATTR_MEDIA_TYPE);
    }

    public IManifest getOwnedManifest() {
        return ownedManifest;
    }

    public String getPath() {
        return implementation.getAttribute(ATTR_FULL_PATH);
    }

    public Object getAdapter(Class adapter) {
        if (adapter == Node.class || adapter == Element.class)
            return implementation;
        return super.getAdapter(adapter);
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof FileEntryImpl))
            return false;
        FileEntryImpl that = (FileEntryImpl) obj;
        return this.implementation == that.implementation;
    }

    public int hashCode() {
        return implementation.hashCode();
    }

    public String toString() {
        return DOMUtils.toString(implementation);
    }

    public int getReferenceCount() {
        return refCount == null ? 0 : refCount.intValue();
    }

    public boolean hasBeenReferred() {
        return refCount != null;
    }

    public void increaseReference() {
        int c = getReferenceCount();
        refCount = Integer.valueOf(c + 1);
        if (c <= 0) {
            ownedManifest.insertFileEntry(this);
        }
        String parent = getParentPath(getPath());
        if (parent != null) {
            IFileEntry parentEntry = ownedManifest.getFileEntry(parent);
            if (parentEntry != null)
                parentEntry.increaseReference();
        }
    }

    public void decreaseReference() {
        if (refCount == null)
            return;
        int c = refCount.intValue();
        c--;
        refCount = Integer.valueOf(c);
        if (c <= 0) {
            ownedManifest.removeFileEntry(this);
        }
        String parent = getParentPath(getPath());
        if (parent != null) {
            IFileEntry parentEntry = ownedManifest.getFileEntry(parent);
            if (parentEntry != null)
                parentEntry.decreaseReference();
        }
    }

//    private IArchivedWorkbook getArchivedWorkbook() {
//        IWorkbook wb = ownedManifest.getOwnedWorkbook();
//        if (wb == null)
//            return null;
//        IArchivedWorkbook aw = ((WorkbookImpl) wb).getTempArchivedWorkbook();
//        if (aw == null)
//            aw = ((WorkbookImpl) wb).getArchivedWorkbook();
//        return aw;
//    }

    private IStorage getStorage() {
        IWorkbook wb = ownedManifest.getOwnedWorkbook();
        if (wb != null)
            return wb.getTempStorage();
        return null;
    }

    public InputStream getInputStream() {
        if (isDirectory())
            return null;
        IStorage storage = getStorage();
        //storag is nothing
        if (storage != null) {
            try {
                IInputSource inputSource = storage.getInputSource();
                String path = getPath();
                return inputSource.getEntryStream(path);
            } catch (CoreException e) {
                Core.getLogger().log(e);
            }
        }
        return null;
//        IArchivedWorkbook aw = getArchivedWorkbook();
//        if (aw != null)
//            return aw.getEntryInputStream(getPath());
//        return null;
    }

    public OutputStream getOutputStream() {
        if (isDirectory())
            return null;
        IStorage storage = getStorage();
        if (storage != null) {
            try {
                return storage.getOutputTarget().getEntryStream(getPath());
            } catch (CoreException e) {
                Core.getLogger().log(e);
            }
        }
//        IArchivedWorkbook aw = getArchivedWorkbook();
//        if (aw != null)
//            return aw.getEntryOutputStream(getPath());
        return null;
    }

    public long getTime() {
        IStorage storage = getStorage();
        if (storage != null)
            try {
                return storage.getInputSource().getEntryTime(getPath());
            } catch (CoreException e) {
                Core.getLogger().log(e);
            }
//        IArchivedWorkbook aw = getArchivedWorkbook();
//        if (aw != null)
//            return aw.getTime(getPath());
        return -1;
    }

    public void setTime(long time) {
        IStorage storage = getStorage();
        if (storage != null) {
            try {
                storage.getOutputTarget().setEntryTime(getPath(), time);
            } catch (CoreException e) {
                Core.getLogger().log(e);
            }
        }
//        IArchivedWorkbook aw = getArchivedWorkbook();
//        if (aw != null)
//            aw.setTime(getPath(), time);
    }

    public List<IFileEntry> getSubEntries() {
        if (!isDirectory())
            return NO_SUB_FILE_ENTRIES;
        String path = getPath();

        Collection<IFileEntry> all = ownedManifest.getAllRegisteredEntries();
        ArrayList<IFileEntry> ret = new ArrayList<IFileEntry>(all);
        Iterator<IFileEntry> it = ret.iterator();
        while (it.hasNext()) {
            IFileEntry e = it.next();
            if (e == this) {
                it.remove();
            } else {
                String p = e.getPath();
                if (!isParentPath(p, path)) {
                    it.remove();
                }
            }
        }
        return ret;
    }

    public boolean isDirectory() {
        return getPath().endsWith("/"); //$NON-NLS-1$
    }

    public long getSize() {
//        IArchivedWorkbook aw = getArchivedWorkbook();
//        if (aw != null) {
//            return aw.getSize(getPath());
//        }
        IStorage storage = getStorage();
        if (storage != null) {
            try {
                return storage.getInputSource().getEntrySize(getPath());
            } catch (CoreException e) {
                Core.getLogger().log(e);
            }
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmind.core.IFileEntry#getEncryptionData()
     */
    public IEncryptionData getEncryptionData() {
        Element encEle = DOMUtils.getFirstChildElementByTag(implementation,
                TAG_ENCRYPTION_DATA);
        if (encEle == null) {
            if (encData != null) {
                encData = null;
            }
        } else {
            if (encData == null) {
                encData = new EncryptionDataImpl(encEle, this);
            }
        }
        return encData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmind.core.IFileEntry#createEncryptionData()
     */
    public IEncryptionData createEncryptionData() {
        IEncryptionData ed = getEncryptionData();
        if (ed != null)
            return ed;

        Element encEle = DOMUtils.createElement(implementation,
                TAG_ENCRYPTION_DATA);
        encData = new EncryptionDataImpl(encEle, this);
        Crypto.initEncryptionData(encData);
        return encData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xmind.core.IFileEntry#deleteEncryptionData()
     */
    public void deleteEncryptionData() {
        if (encData != null) {
            Element encEle = encData.getImplementation();
            if (encEle != null && encEle.getParentNode() == implementation) {
                implementation.removeChild(encEle);
            }
            encData = null;
        }
    }

    /**
     * @param ignoreEncryption
     *            the ignoreEncryption to set
     */
    public void setIgnoreEncryption(boolean ignoreEncryption) {
        this.ignoreEncryption = ignoreEncryption;
    }

    /**
     * @return the ignoreEncryption
     */
    public boolean isIgnoreEncryption() {
        return ignoreEncryption;
    }

}