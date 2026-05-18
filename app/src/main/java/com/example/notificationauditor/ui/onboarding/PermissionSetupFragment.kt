package com.example.notificationauditor.ui.onboarding

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notificationauditor.R
import com.example.notificationauditor.service.NotificationHarvesterService

class PermissionSetupFragment : Fragment() {

    private val postNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshUi() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_permission_setup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btn_grant_nls).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        view.findViewById<Button>(R.id.btn_grant_usage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        view.findViewById<Button>(R.id.btn_continue).setOnClickListener {
            requestPostNotificationIfNeeded()
            if (allGranted()) {
                findNavController().navigate(R.id.action_setup_to_dashboard)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val allGranted = allGranted()
        view?.findViewById<Button>(R.id.btn_continue)?.isEnabled = allGranted
        if (allGranted) {
            findNavController().navigate(R.id.action_setup_to_dashboard)
        }
    }

    private fun allGranted(): Boolean =
        isNlsGranted() && isUsageStatsGranted()

    private fun isNlsGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(requireContext(), NotificationHarvesterService::class.java)
        return enabledListeners.contains(component.flattenToString())
    }

    private fun isUsageStatsGranted(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestPostNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED
            if (!granted) {
                postNotifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
