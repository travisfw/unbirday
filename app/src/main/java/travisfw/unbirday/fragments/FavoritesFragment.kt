package travisfw.unbirday.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import travisfw.unbirday.R
import travisfw.unbirday.activities.MainActivity
import travisfw.unbirday.adapters.FavoritesAdapter
import travisfw.unbirday.animators.UnbirdayRecyclerAnimator
import travisfw.unbirday.databinding.DialogNotesBinding
import travisfw.unbirday.databinding.FragmentFavoritesBinding
import travisfw.unbirday.model.Event
import travisfw.unbirday.utilities.addInsetsByPadding
import travisfw.unbirday.viewmodels.MainViewModel
import androidx.core.content.edit


class FavoritesFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FavoritesAdapter
    private lateinit var act: MainActivity
    private lateinit var sharedPrefs: SharedPreferences
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private var _dialogNotesBinding: DialogNotesBinding? = null
    private val dialogNotesBinding get() = _dialogNotesBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FavoritesAdapter(
            onItemClick = { position -> onItemClick(position) },
            onItemLongClick = { position -> onItemLongClick(position) }
        )
        act = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Activate the overscroll effect on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.favoritesRecycler.overScrollMode = View.OVER_SCROLL_ALWAYS
        }

        // Setup the recycler view
        val recycler = binding.favoritesRecycler
        recycler.adapter = adapter
        with(mainViewModel) {
            getFavorites().observe(viewLifecycleOwner) { events ->
                // Update the cached copy in the adapter
                if (events != null && events.isNotEmpty()) {
                    removePlaceholder()
                    adapter.submitList(events)
                    recycler.itemAnimator = UnbirdayRecyclerAnimator()
                }
                if (events.isNullOrEmpty()) {
                    adapter.submitList(emptyList())
                    restorePlaceholder()
                }
            }
        }

        // Add insets
        recycler.addInsetsByPadding(bottom = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
        _dialogNotesBinding = null
    }

    private fun onItemClick(position: Int) {
        act.vibrate()
        _dialogNotesBinding = DialogNotesBinding.inflate(LayoutInflater.from(context))
        val event = adapter.getItem(position)
        val notesTitle = "${getString(R.string.notes)} - ${event.name}"
        val noteTextField = dialogNotesBinding.favoritesNotes
        noteTextField.setText(event.notes)

        // Native dialog
        MaterialAlertDialogBuilder(act)
            .setView(dialogNotesBinding.root)
            .setTitle(notesTitle)
            .setIcon(R.drawable.ic_note_24dp)
            .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                val note = noteTextField.text.toString().trim()
                val tuple = Event(
                    id = event.id,
                    type = event.type,
                    originalDate = event.originalDate,
                    name = event.name,
                    yearMatter = event.yearMatter,
                    surname = event.surname,
                    favorite = event.favorite,
                    notes = note,
                    image = event.image
                )
                mainViewModel.update(tuple)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Open the details screen on long press, just another shortcut
    private fun onItemLongClick(position: Int) {
        // Return if there was a navigation, useful to avoid double tap on two events
        if (findNavController().currentDestination?.label != "fragment_favorites")
            return
        act.vibrate()
        // Cast required to obtain the original event result from the event item wrapper
        val event = adapter.getItem(position)

        // Navigate to the new fragment passing in the event with safe args
        val action =
            FavoritesFragmentDirections.actionNavigationFavoritesToDetailsFragment(event, position)
        findNavController().navigate(action)
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder = binding.noFavorites
        placeholder.visibility = View.GONE
    }

    // Restore the placeholder
    private fun restorePlaceholder() {
        val placeholder = binding.noFavorites
        placeholder.visibility = View.VISIBLE
    }
}
