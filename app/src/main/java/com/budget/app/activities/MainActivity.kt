package com.budget.app.activities

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import android.widget.Toast
import com.budget.app.R
import com.budget.app.fragments.*
import com.budget.app.utils.AppData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load Dark Mode Preference
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AppData with persistence
        AppData.init(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        bottomNav = findViewById(R.id.bottomNavigationView)

        // Update Nav Header
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvNavEmail).text = AppData.currentUser?.email ?: ""

        // Show dashboard on start
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), addToBackStack = false)
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard    -> DashboardFragment()
                R.id.nav_add          -> AddTransactionFragment()
                R.id.nav_profile      -> ProfileFragment()
                else -> DashboardFragment()
            }
            loadFragment(fragment)
            true
        }

        // Handle Back Press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }

                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is OnBackPressedListener) {
                    if (currentFragment.onBackPressed()) {
                        return // Fragment handled it
                    }
                }

                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    supportFragmentManager.executePendingTransactions()
                    android.os.Handler(android.os.Looper.getMainLooper()).post { updateBottomNavSelection() }
                } else {
                    if (currentFragment is DashboardFragment) {
                        if (System.currentTimeMillis() - backPressedTime < 2000) {
                            moveTaskToBack(true)
                        } else {
                            backPressedTime = System.currentTimeMillis()
                            Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        navigateTo(R.id.nav_dashboard)
                    }
                }
            }
        })
    }

    private fun updateBottomNavSelection() {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        when (current) {
            is DashboardFragment -> bottomNav.menu.findItem(R.id.nav_dashboard).isChecked = true
            is AddTransactionFragment -> bottomNav.menu.findItem(R.id.nav_add).isChecked = true
            is ProfileFragment -> bottomNav.menu.findItem(R.id.nav_profile).isChecked = true
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigateTo(item.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (current?.javaClass == fragment.javaClass) return

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
        
        if (addToBackStack) {
            transaction.addToBackStack(fragment.javaClass.simpleName)
        }
        
        transaction.commit()
    }

    fun navigateTo(itemId: Int) {
        // First check if it's a bottom nav item to sync UI
        if (itemId == R.id.nav_dashboard || itemId == R.id.nav_add || itemId == R.id.nav_profile) {
            bottomNav.selectedItemId = itemId
            return
        }

        // Otherwise load manually
        val fragment: Fragment = when (itemId) {
            R.id.nav_transactions -> TransactionsFragment()
            R.id.nav_attachments  -> AttachmentsFragment()
            R.id.nav_budget_goals -> BudgetGoalsFragment()
            R.id.nav_goals        -> FinancialGoalsFragment()
            R.id.nav_debts        -> DebtReductionFragment()
            R.id.nav_education    -> EducationFragment()
            R.id.nav_reports      -> ReportFragment()
            R.id.nav_categories   -> CategoriesFragment()
            R.id.nav_settings     -> SettingsFragment()
            else -> DashboardFragment()
        }
        loadFragment(fragment)
    }

    interface OnBackPressedListener {
        fun onBackPressed(): Boolean
    }
}
