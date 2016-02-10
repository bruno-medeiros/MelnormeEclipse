/*******************************************************************************
 * Copyright (c) 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.core.engine;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.utils.CoreExecutors;
import melnorme.lang.ide.core.utils.DefaultBufferListener;
import melnorme.utilbox.misc.StringUtil;
import melnorme.utilbox.ownership.LifecycleObject;

public class AutoSaveManager extends LifecycleObject {
	
	protected final ExecutorService executor = CoreExecutors.newExecutorTaskAgent(getClass());
	
	public AutoSaveManager() {
		this(true);
	}
	
	public AutoSaveManager(boolean initialize) {
		if(initialize) {
			initialize();
		}
	}
	
	public void initialize() {
		ITextFileBufferManager fbm = FileBuffers.getTextFileBufferManager();
		fbm.addFileBufferListener(fbmListener);
		
		// setup the FBM dispose code:
		owned.add(() -> fbm.removeFileBufferListener(fbmListener));
	}
	
	protected final DefaultBufferListener fbmListener = new DefaultBufferListener() {
		@Override
		public void bufferCreated(IFileBuffer buffer) {
			if(buffer instanceof ITextFileBuffer) {
				ITextFileBuffer textFileBuffer = (ITextFileBuffer) buffer;
				IDocument doc = textFileBuffer.getDocument();
				IDocumentListener docListener = new DocumentAutoSaveListener(textFileBuffer);
				doc.addDocumentListener(docListener);
			}
		}
		
	};
	
	protected class DocumentAutoSaveListener implements IDocumentListener {
		
		protected final ITextFileBuffer textFileBuffer;
		protected volatile UpdaterTask currentUpdateTask;
		
		public DocumentAutoSaveListener(ITextFileBuffer textFileBuffer) {
			this.textFileBuffer = assertNotNull(textFileBuffer);
		}
		
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			IDocument document = event.fDocument;
			
			long modificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
			if (document instanceof IDocumentExtension4) {
				modificationStamp = ((IDocumentExtension4)document).getModificationStamp();
			}
			
			String contents = document.get();
			
			// Usually synchronized block should not be necessary, given #documentChanged is triggered
			// under document lock. But just in case the document has no lock.
			synchronized (this) {
				
				if(currentUpdateTask != null) {
					currentUpdateTask.cancel();
				}
				UpdaterTask newUpdaterTask = new UpdaterTask(textFileBuffer, contents, modificationStamp);
				executor.submit(newUpdaterTask);
			}
			
		}
		
	}
	
	protected class UpdaterTask implements Runnable {
		
		protected final ITextFileBuffer textFileBuffer;
		protected final ByteArrayInputStream contents;
		protected final long modificationStamp;
		protected final IProgressMonitor pm = new NullProgressMonitor();
		
		public UpdaterTask(ITextFileBuffer textFileBuffer, String contents, long modificationStamp) {
			this.textFileBuffer = textFileBuffer;
			/* FIXME: todo hasBOM and UTF BOM in stream */
			this.contents = new ByteArrayInputStream(contents.getBytes(StringUtil.UTF8));
			this.modificationStamp = modificationStamp;
		}
		
		public void cancel() {
			pm.setCanceled(true);
		}
		
		@Override
		public void run() {
			try {
				//System.out.println("  commitTextFileBuffer: " + contents.length());
				
				textFileBuffer.internalCommitFileContents(pm, false, contents, modificationStamp, false);
				
				//System.out.println("finished update");
			} catch(CoreException e) {
				LangCore.logStatus(e);
			}
		}
		
	}
	
}