package travisfw.unbirday.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import travisfw.unbirday.activities.MainActivity
import travisfw.unbirday.model.ContactInfo
import travisfw.unbirday.persistence.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalStdlibApi
class InsertEventViewModel(application: Application) : AndroidViewModel(application) {
    private val contactsRepository = ContactsRepository()

    private val _contactsList = MutableLiveData<List<ContactInfo>>(emptyList())

    // Don't expose the Mutable version explicitly
    val contactsList: LiveData<List<ContactInfo>> = _contactsList

    @SuppressLint("MissingPermission")
    fun initContactsList(act: MainActivity) {
        if (act.askContactsPermission()) {

            val resolver = getApplication<Application>().contentResolver
            viewModelScope.launch {
                val contacts = withContext(Dispatchers.IO) {
                    contactsRepository.queryContacts(resolver)
                }
                _contactsList.postValue(contacts)
            }
        }
    }
}