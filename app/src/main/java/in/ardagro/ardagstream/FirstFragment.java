package in.ardagro.ardagstream;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Calendar;

public class FirstFragment extends Fragment {
AlarmManager alarmManager;
PendingIntent pendingIntent;
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent uploadcamshotSerRec = new Intent(getActivity().getApplicationContext(),StreamBroadCastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getActivity(),0,uploadcamshotSerRec,0);
        //pendingIntent
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              startService();
            }
        });
    }

    private void  startService(){
     alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ((Calendar.getInstance().getTimeInMillis()) +1000*60),pendingIntent);

    }
}
