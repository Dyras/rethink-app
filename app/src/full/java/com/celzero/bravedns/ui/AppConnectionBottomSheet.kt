/*
 * Copyright 2020 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.celzero.bravedns.ui

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.celzero.bravedns.R
import com.celzero.bravedns.adapter.AppConnectionAdapter
import com.celzero.bravedns.databinding.BottomSheetAppConnectionsBinding
import com.celzero.bravedns.service.IpRulesManager
import com.celzero.bravedns.service.PersistentState
import com.celzero.bravedns.util.Constants.Companion.INVALID_UID
import com.celzero.bravedns.util.Constants.Companion.UNSPECIFIED_PORT
import com.celzero.bravedns.util.Themes.Companion.getBottomsheetCurrentTheme
import com.celzero.bravedns.util.Utilities
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class AppConnectionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAppConnectionsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val b
        get() = _binding!!

    private val persistentState by inject<PersistentState>()

    // listener to inform dataset change to the adapter
    private var dismissListener: OnBottomSheetDialogFragmentDismiss? = null
    private var adapter: AppConnectionAdapter? = null
    private var position: Int = -1

    override fun getTheme(): Int =
        getBottomsheetCurrentTheme(isDarkThemeOn(), persistentState.theme)

    private var uid: Int = -1
    private var ipAddress: String = ""
    private var port: Int = UNSPECIFIED_PORT
    private var rule: IpRulesManager.IpRuleStatus = IpRulesManager.IpRuleStatus.NONE

    companion object {
        const val UID = "UID"
        const val IP_ADDRESS = "IP_ADDRESS"
        const val PORT = "PORT"
        const val IP_RULE = "IP_RULE"
    }

    private fun isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    interface OnBottomSheetDialogFragmentDismiss {
        fun notifyDataset(position: Int)
    }

    fun dismissListener(aca: AppConnectionAdapter?, pos: Int) {
        adapter = aca
        position = pos
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppConnectionsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uid = arguments?.getInt(UID) ?: INVALID_UID
        ipAddress = arguments?.getString(IP_ADDRESS) ?: ""
        port = arguments?.getInt(PORT) ?: UNSPECIFIED_PORT
        val status = arguments?.getInt(IP_RULE) ?: IpRulesManager.IpRuleStatus.NONE.id
        rule = IpRulesManager.IpRuleStatus.getStatus(status)
        dismissListener = adapter

        initView()
        initializeClickListeners()
    }

    private fun initView() {
        if (uid == INVALID_UID) {
            this.dismiss()
            return
        }
        b.bsacHeading.text = ipAddress

        when (rule) {
            IpRulesManager.IpRuleStatus.NONE -> showButtonsForStatusNone()
            IpRulesManager.IpRuleStatus.BYPASS_UNIVERSAL -> {
                // no-op, bypass universal rules don't apply in app specific list
            }
            IpRulesManager.IpRuleStatus.TRUST -> showByPassAppRulesUi()
            IpRulesManager.IpRuleStatus.BLOCK -> showButtonForStatusBlock()
        }
    }

    override fun onResume() {
        super.onResume()
        if (uid == INVALID_UID) {
            this.dismiss()
            return
        }
    }

    private fun showButtonForStatusBlock() {
        b.bsacUnblock.visibility = View.VISIBLE
        b.bsacTrustIp.visibility = View.VISIBLE
    }

    private fun showByPassAppRulesUi() {
        b.bsacBlock.visibility = View.VISIBLE
        b.bsacDistrustIp.visibility = View.VISIBLE
    }

    private fun showButtonsForStatusNone() {
        b.bsacBlock.visibility = View.VISIBLE
        b.bsacTrustIp.visibility = View.VISIBLE
    }

    private fun initializeClickListeners() {
        b.bsacBlock.setOnClickListener {
            applyRule(
                uid,
                ipAddress,
                IpRulesManager.IpRuleStatus.BLOCK,
                getString(R.string.bsac_block_toast, ipAddress)
            )
        }

        b.bsacUnblock.setOnClickListener {
            applyRule(
                uid,
                ipAddress,
                IpRulesManager.IpRuleStatus.NONE,
                getString(R.string.bsac_unblock_toast, ipAddress)
            )
        }

        b.bsacTrustIp.setOnClickListener {
            applyRule(
                uid,
                ipAddress,
                IpRulesManager.IpRuleStatus.TRUST,
                getString(R.string.bsac_trust_toast, ipAddress)
            )
        }

        b.bsacDistrustIp.setOnClickListener {
            applyRule(
                uid,
                ipAddress,
                IpRulesManager.IpRuleStatus.NONE,
                getString(R.string.bsac_distrust_toast, ipAddress)
            )
        }
    }

    private fun applyRule(
        uid: Int,
        ipAddress: String,
        status: IpRulesManager.IpRuleStatus,
        toastMsg: String
    ) {
        // set port number as UNSPECIFIED_PORT for all the rules applied from this screen
        io { IpRulesManager.addIpRule(uid, ipAddress, UNSPECIFIED_PORT, status) }
        Utilities.showToastUiCentered(requireContext(), toastMsg, Toast.LENGTH_SHORT)
        this.dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.notifyDataset(position)
    }

    private fun io(f: suspend () -> Unit) {
        lifecycleScope.launch { withContext(Dispatchers.IO) { f() } }
    }
}
