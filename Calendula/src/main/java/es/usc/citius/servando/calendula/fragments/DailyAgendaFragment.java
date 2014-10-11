package es.usc.citius.servando.calendula.fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tjerkw.slideexpandable.library.SlideExpandableListAdapter;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import es.usc.citius.servando.calendula.HomeActivity;
import es.usc.citius.servando.calendula.R;
import es.usc.citius.servando.calendula.activities.AgendaDetailActivity;
import es.usc.citius.servando.calendula.activities.ScheduleCreationActivity;
import es.usc.citius.servando.calendula.util.DailyAgendaItemStub;
import es.usc.citius.servando.calendula.util.RandomColorChooser;

/**
 * Created by joseangel.pineiro on 11/15/13.
 */
public class DailyAgendaFragment extends Fragment {

    List<DailyAgendaItemStub> items = new ArrayList<DailyAgendaItemStub>();
    HomeUserInfoFragment userProInfoFragment;

    ArrayAdapter adapter = null;
    ListView listview = null;

    int lastScroll = 0;
    int lastFirstVisibleItem = 0;

    boolean profileShown = true;
    View userInfoFragment;

    int profileFragmentHeight = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_daily_agenda, container, false);
        listview = (ListView) rootView.findViewById(R.id.listview);

        inflater.inflate(R.layout.fragment_edit_profile, container, false);
        userProInfoFragment=HomeUserInfoFragment.newInstance();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.user_info_fragment,userProInfoFragment)
                .commit();

        userInfoFragment = rootView.findViewById(R.id.user_info_fragment);
        items = buildItems(); // allow user to change day
        adapter = new AgendaItemAdapter(getActivity(), R.layout.daily_view_hour, items);
        listview.setAdapter(new SlideExpandableListAdapter(adapter, R.id.count_container, R.id.bottom));
        profileFragmentHeight = userInfoFragment.getHeight();

        listview.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                listview.computeScroll();
                int scrollPosition = listview.getScrollY();
                Log.d(getTag(), "Scroll Y: " + scrollPosition + " firstItem: " + firstVisibleItem);

                if (firstVisibleItem > 1 && firstVisibleItem > lastFirstVisibleItem) {
                    hideProfile();
                } else if (firstVisibleItem < lastFirstVisibleItem) {
                    showProfile();
                }

                lastFirstVisibleItem = firstVisibleItem;
                lastScroll = scrollPosition;
            }
        });

        return rootView;
    }

    private void launchActivity(Class activityCls) {
        Intent intent = new Intent(getActivity(), activityCls);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        items = buildItems(); // allow user to change day
        int currentHour = DateTime.now().getHourOfDay();
        adapter.notifyDataSetChanged();
        //listview.smoothScrollToPosition(currentHour);
    }

    public List<DailyAgendaItemStub> buildItems() {

        ArrayList<DailyAgendaItemStub> items = new ArrayList<DailyAgendaItemStub>();

        addSpacerTop(items);

        for (int i = 0; i < 24; i++) {
            DailyAgendaItemStub item = DailyAgendaItemStub.fromRoutine(i);
            items.add(item);
        }
        return items;
    }

    private void addSpacerTop(ArrayList<DailyAgendaItemStub> items) {
        DailyAgendaItemStub spacer = new DailyAgendaItemStub(0);
        spacer.isSpacer=true;
        items.add(spacer);
    }


    private class AgendaItemAdapter extends ArrayAdapter<DailyAgendaItemStub> {

        public AgendaItemAdapter(Context context, int layoutResourceId, List<DailyAgendaItemStub> items) {
            super(context, layoutResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();

            final int hour = new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
            int minute = new GregorianCalendar().get(Calendar.MINUTE);

            final DailyAgendaItemStub item = items.get(position);


            View v;

            if (!item.isSpacer) {
                if (!item.hasColors) {
                    int colorIndex = RandomColorChooser.getFixedColorIdx(new Integer(item.hour));
                    item.primaryColor = RandomColorChooser.getPrimaryColor(colorIndex, getResources());
                    item.secondaryColor = RandomColorChooser.getSecondaryColor(colorIndex, getResources());
                    item.hasColors = true;
                }
                v = layoutInflater.inflate(R.layout.daily_view_hour, null);

                // select the correct layout
                if (!item.hasEvents) {
                    v.findViewById(R.id.hide_if_empty).setVisibility(View.GONE);
                    v.findViewById(R.id.current_hour_spacer).setVisibility(View.VISIBLE);
                } else {
                    LinearLayout medList = (LinearLayout) v.findViewById(R.id.med_item_list);
                    boolean isFirst = true;
                    for (DailyAgendaItemStub.DailyAgendaItemStubElement element : item.meds) {

                        View medNameView = layoutInflater.inflate(R.layout.daily_agenda_item_med, null);
                        ((TextView) medNameView.findViewById(R.id.med_item_name)).setText(element.medName + (element.taken ? " ✔" : ""));
                        if (isFirst) {
                            ((TextView) medNameView.findViewById(R.id.bottom_current_hour_text)).setText(String.valueOf((item.hour < 10 ? ("0" + item.hour) : item.hour)));
                            isFirst = false;
                        } else {
                            medNameView.findViewById(R.id.bottom_current_hour_text).setVisibility(View.INVISIBLE);
                        }
                        // change colors
                        medNameView.setBackgroundColor(item.primaryColor);
                        ((TextView) medNameView.findViewById(R.id.bottom_current_hour_text)).setTextColor(item.primaryColor);
                        ((TextView) medNameView.findViewById(R.id.bottom_current_minute_text)).setTextColor(item.secondaryColor);

                        ((TextView) medNameView.findViewById(R.id.bottom_current_minute_text)).setText(element.minute);
                        medList.addView(medNameView);
                    }

                    View moreView = layoutInflater.inflate(R.layout.daily_agenda_item_more, null);
                    moreView.setBackgroundColor(item.primaryColor);
                    moreView.findViewById(R.id.more_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Intent intent = new Intent(getContext(), AgendaDetailActivity.class);
                            intent.putExtra("hour", item.hour);
                            startActivity(intent);
                            getActivity().overridePendingTransition(0, 0);
                        }
                    });
                    medList.addView(moreView);
                    // set number of meds to take
                    ((TextView) v.findViewById(R.id.count_text)).setText(String.valueOf(item.meds.size()));

                }

                // enable hour indicator
                if (hour == item.hour) {
                    v.findViewById(R.id.hour_text).setVisibility(View.INVISIBLE);
                    v.findViewById(R.id.current_hour_indicator).setVisibility(View.VISIBLE);
                    TextView hText = (TextView) v.findViewById(R.id.current_hour_text);
                    TextView mText = (TextView) v.findViewById(R.id.current_minute_text);
                    hText.setText(String.valueOf(hour));
                    mText.setText(":" + String.valueOf(minute));
                    hText.setVisibility(View.VISIBLE);
                    mText.setVisibility(View.VISIBLE);
                    v.findViewById(R.id.current_minute_text).setVisibility(View.VISIBLE);
                }

                // change background color
                v.findViewById(R.id.agenda_item_top_bg).setBackgroundColor(item.primaryColor);
                v.findViewById(R.id.item_spacer_top).setBackgroundColor(item.primaryColor);
                v.findViewById(R.id.item_spacer_bottom).setBackgroundColor(item.primaryColor);
                // set hour label
                ((TextView) v.findViewById(R.id.hour_text)).setText(String.valueOf(item.hour));
            } else {
                v =  layoutInflater.inflate(R.layout.daily_view_spacer, null);
            }
            return v;
        }
    }


    public void hideProfile() {
        if (profileShown) {
            profileShown = false;
            Animation slideUp = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_slide_up);
            slideUp.setFillAfter(true);
            userInfoFragment.startAnimation(slideUp);
            ((HomeActivity)getActivity()).hideAddButton();
        }
    }

    public void showProfile() {
        if (!profileShown) {
            profileShown = true;
            Animation slideDown = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_slide_down);
            slideDown.setFillAfter(true);
            userInfoFragment.startAnimation(slideDown);
            ((HomeActivity)getActivity()).showAddButton();

        }
    }


}