package com.example.regainassignment.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner

import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.regainassignment.ui.overlay.BlockOverlayContent
import com.example.regainassignment.ui.overlay.TimeUpContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor() {

    private var windowManager: WindowManager? = null
    private var blockerLayout: ComposeView? = null
    private var timerLayout: ComposeView? = null
    private var context: Context? = null

    // For lifecycle management in ComposeView attached to WindowManager
    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        init {
            // Restore MUST happen before state is RESUMED and while state is INITIALIZED (default)
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    }

    fun init(ctx: Context) {
        context = ctx
        windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showBlocker(
        dailyUsage: String,
        appName: String,
        onTimeSelected: (Int) -> Unit
    ) {
        if (blockerLayout != null) return // Already showing
        val ctx = context ?: return

        blockerLayout = ComposeView(ctx).apply {
            val lifecycleOwner = OverlayLifecycleOwner()
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeViewModelStoreOwner( object : androidx.lifecycle.ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            })
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            setContent {
                BlockOverlayContent(
                    dailyUsage = dailyUsage,
                    onTimeSelected = { 
                        onTimeSelected(it)
                        hideBlocker()
                    },
                    appName = appName
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL // Align to bottom
            // Ensure input mode handles back key if needed, but accessibility overlay often intercepts.
        }

        try {
            windowManager?.addView(blockerLayout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hideBlocker() {
        blockerLayout?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) { e.printStackTrace() }
            blockerLayout = null
        }
    }
    
    // Timer removed as requested (moved to notification)
    fun hideTimer() {
        // No-op or cleanup if legacy exists
    }

    fun showTimeUp(
        appName: String,
        usageMinutes: String,
        onClose: () -> Unit,
        onOpenAnyway: () -> Unit
    ) {
         if (blockerLayout != null) {
             hideBlocker() // Reusing blocker variable or creating new?
         }
         // Reuse blocker view logic
         val ctx = context ?: return
         
         blockerLayout = ComposeView(ctx).apply {
            val lifecycleOwner = OverlayLifecycleOwner()
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeViewModelStoreOwner(object : androidx.lifecycle.ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            })
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                TimeUpContent(
                    appName = appName,
                    usageMinutes = usageMinutes,
                    onClose = {
                        onClose()
                        hideBlocker()
                    },
                    onOpenAnyway = {
                        onOpenAnyway()
                        hideBlocker()
                    }
                )
            }
        }
         val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
        }
        try {
            windowManager?.addView(blockerLayout, params)
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun showTemporaryBlockOverlay(
        appName: String,
        dailyUsage: String,
        onTimeSelected: (Int) -> Unit,
        onClose: () -> Unit
    ) {
        if (blockerLayout != null) hideBlocker() // Remove any existing overlay
        val ctx = context ?: return
        
        blockerLayout = ComposeView(ctx).apply {
            val lifecycleOwner = OverlayLifecycleOwner()
            this.setViewTreeLifecycleOwner(lifecycleOwner)
            this.setViewTreeViewModelStoreOwner(object : androidx.lifecycle.ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            })
            this.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            setContent {
                com.example.regainassignment.ui.overlay.TemporaryBlockContent(
                    appName = appName,
                    dailyUsage = dailyUsage,
                    onTimeSelected = {
                        onTimeSelected(it)
                        hideBlocker()
                    },
                    onClose = {
                        onClose()
                        hideBlocker()
                    }
                )
            }
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
        }
        
        try {
            windowManager?.addView(blockerLayout, params)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
