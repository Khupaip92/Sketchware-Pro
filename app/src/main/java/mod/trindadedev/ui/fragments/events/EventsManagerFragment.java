package mod.trindadedev.ui.fragments.events;

import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.sketchware.remod.R;
import com.sketchware.remod.databinding.DialogAddNewListenerBinding;
import com.sketchware.remod.databinding.FragmentEventsManagerBinding;
import com.sketchware.remod.databinding.LayoutEventItemBinding;

import mod.trindadedev.ui.fragments.BaseFragment;
import mod.SketchwareUtil;
import mod.agus.jcoderz.lib.FileUtil;
import mod.hey.studios.util.Helper;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;

public class EventsManagerFragment extends BaseFragment {

    private FragmentEventsManagerBinding binding;
    public static final File EVENT_EXPORT_LOCATION = new File(Environment.getExternalStorageDirectory(),
            ".sketchware/data/system/export/events/");
    public static final File EVENTS_FILE = new File(Environment.getExternalStorageDirectory(),
            ".sketchware/data/system/events.json");
    public static final File LISTENERS_FILE = new File(Environment.getExternalStorageDirectory(),
            ".sketchware/data/system/listeners.json");
    private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventsManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureToolbar(binding.toolbar);
        binding.activityEventsCard.setOnClickListener(v -> openFragment(new EventsManagerDetailsFragment()));
        binding.fabNewListener.setOnClickListener(v -> showAddNewListenerDialog());
        refreshList();
    }

    private void showAddNewListenerDialog() {
        showListenerDialog(null, -1);
    }

    private void showEditListenerDialog(int position) {
        showListenerDialog(listMap.get(position), position);
    }

    private void showListenerDialog(@Nullable HashMap<String, Object> existingListener, int position) {
        var listenerBinding = DialogAddNewListenerBinding.inflate(LayoutInflater.from(requireContext()));
        if (existingListener != null) {
            listenerBinding.listenerName.setText(existingListener.get("name").toString());
            listenerBinding.listenerCode.setText(existingListener.get("code").toString());
            listenerBinding.listenerCustomImport.setText(existingListener.get("imports").toString());
            if ("true".equals(existingListener.get("s"))) {
                listenerBinding.listenerIsIndependentClassOrMethod.setChecked(true);
                listenerBinding.listenerCode.setText(
                        existingListener.get("code").toString().replaceFirst("//" + listenerBinding.listenerName.getText().toString() + "\n", ""));
            }
        }

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingListener == null ? "New Listener" : "Edit Listener")
                .setMessage("Type info of the listener")
                .setView(listenerBinding.getRoot())
                .setPositiveButton("Save", (di, i) -> {
                    String listenerName = listenerBinding.listenerName.getText().toString();
                    if (!listenerName.isEmpty()) {
                        HashMap<String, Object> hashMap = existingListener != null ? existingListener : new HashMap<>();
                        hashMap.put("name", listenerName);
                        hashMap.put("code", listenerBinding.listenerIsIndependentClassOrMethod.isChecked()
                                ? "//" + listenerName + "\n" + listenerBinding.listenerCode.getText().toString()
                                : listenerBinding.listenerCode.getText().toString());
                        hashMap.put("s", listenerBinding.listenerIsIndependentClassOrMethod.isChecked() ? "true" : "false");
                        hashMap.put("imports", listenerBinding.listenerCustomImport.getText().toString());
                        if (position >= 0) {
                            listMap.set(position, hashMap);
                        } else {
                            listMap.add(hashMap);
                        }
                        addListenerItem();
                        di.dismiss();
                    } else {
                        SketchwareUtil.toastError("Invalid name!");
                    }
                })
                .setNegativeButton("Cancel", (di, i) -> di.dismiss()).create();
        dialog.show();
    }

    public void refreshList() {
        listMap.clear();
        if (FileUtil.isExistFile(LISTENERS_FILE.getAbsolutePath())) {
            listMap = new Gson().fromJson(FileUtil.readFile(LISTENERS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            binding.listenersList.setAdapter(new ListenersAdapter(listMap, requireContext()));
            binding.listenersList.getAdapter().notifyDataSetChanged();
        }
    }

    private void addListenerItem() {
        FileUtil.writeFile(LISTENERS_FILE.getAbsolutePath(), new Gson().toJson(listMap));
        refreshList();
    }

    public class ListenersAdapter extends RecyclerView.Adapter<ListenersAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> dataArray;
        private final Context context;

        public ListenersAdapter(ArrayList<HashMap<String, Object>> arrayList, Context context) {
            this.dataArray = arrayList;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutEventItemBinding binding = LayoutEventItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, Object> item = dataArray.get(position);
            String name = (String) item.get("name");

            holder.binding.eventIcon.setImageResource(R.drawable.event_on_response_48dp);
            ((LinearLayout) holder.binding.eventIcon.getParent()).setGravity(Gravity.CENTER);

            holder.binding.eventTitle.setText(name);
            holder.binding.eventSubtitle.setText(getNumOfEvents(name));

            holder.binding.eventCard.setOnClickListener(v -> {});

            holder.binding.eventCard.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(name)
                        .setItems(new String[]{"Edit", "Export", "Delete"}, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    showEditListenerDialog(position);
                                    break;
                                case 1:
                                    // TODO: export(position);
                                    break;
                                case 2:
                                    // TODO: deleteRelatedEvents(name);
                                    // TODO: deleteItem(position);
                                    break;
                            }
                        }).show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return dataArray.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final LayoutEventItemBinding binding;

            public ViewHolder(@NonNull LayoutEventItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public static String getNumOfEvents(String name) {
        int eventAmount = 0;
        if (FileUtil.isExistFile(EVENTS_FILE.getAbsolutePath())) {
            ArrayList<HashMap<String, Object>> events = new Gson()
                    .fromJson(FileUtil.readFile(EVENTS_FILE.getAbsolutePath()), Helper.TYPE_MAP_LIST);
            for (HashMap<String, Object> event : events) {
                if (event.get("listener").toString().equals(name)) {
                    eventAmount++;
                }
            }
        }
        return "Events: " + eventAmount;
    }
}
