package cy.org.cyens.reinherit;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayDeque;
import java.util.Queue;

public class SharedViewModel extends ViewModel {
    MutableLiveData<Queue<String>> messagesToBluetooth = new MutableLiveData<>();
    MutableLiveData<Queue<String>> messagesFromBluetooth = new MutableLiveData<>();

    public SharedViewModel() {
        super();
        messagesFromBluetooth.setValue(new ArrayDeque<>());
        messagesToBluetooth.setValue(new ArrayDeque<>());
    }

    public void sendMessage(String msg){
        Queue<String> messages = messagesToBluetooth.getValue();
        messages.add(msg);
        messagesToBluetooth.postValue(messages);
    }
}
