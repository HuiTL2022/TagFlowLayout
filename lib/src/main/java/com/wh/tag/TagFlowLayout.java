package com.wh.tag;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author: wh
 * @date: 2022/12/17 11:51
 * @desc:
 */
public class TagFlowLayout extends FlowLayout implements TagAdapter.OnDataChangedListener {

    private TagAdapter mTagAdapter;
    private int mSelectedMax = -1;//-1为不限制数量
    private boolean mTagClickable;//-1为不限制数量
    private static final String TAG = "TagFlowLayout";

    private Set<Integer> mSelectedView = new HashSet<Integer>();
    private OnSelectListener mOnSelectListener;
    private OnTagClickListener mOnTagClickListener;

    public TagFlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public TagFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TagFlowLayout(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TagFlowLayout);
        mSelectedMax = ta.getInt(R.styleable.TagFlowLayout_max_select, -1);
        mTagClickable = ta.getBoolean(R.styleable.TagFlowLayout_tag_clickable, true);
        ta.recycle();
    }

    public interface OnSelectListener {
        void onSelected(Set<Integer> selectPosSet);
    }

    public interface OnTagClickListener {
        boolean onTagClick(View view, int position, FlowLayout parent);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int cCount = getChildCount();
        for (int i = 0; i < cCount; i++) {
            TagView tagView = (TagView) getChildAt(i);
            if (tagView.getVisibility() == View.GONE) {
                continue;
            }
            if (tagView.getTagView().getVisibility() == View.GONE) {
                tagView.setVisibility(View.GONE);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void setOnSelectListener(OnSelectListener onSelectListener) {
        mOnSelectListener = onSelectListener;
    }


    public void setOnTagClickListener(OnTagClickListener onTagClickListener) {
        mOnTagClickListener = onTagClickListener;
    }

    public void setAdapter(TagAdapter adapter) {
        mTagAdapter = adapter;
        mTagAdapter.setOnDataChangedListener(this);
        mSelectedView.clear();
        changeAdapter();
    }

    private void changeAdapter() {
        super.removeAllViews();
        TagAdapter adapter = mTagAdapter;
        TagView tagViewContainer = null;
        HashSet preCheckedList = mTagAdapter.getPreCheckedList();
        for (int i = 0; i < adapter.getCount(); i++) {
            View tagView = adapter.getView(this, i, adapter.getItem(i));

            tagViewContainer = new TagView(super.getContext());
            tagView.setDuplicateParentStateEnabled(true);
            if (tagView.getLayoutParams() != null) {
                tagViewContainer.setLayoutParams(tagView.getLayoutParams());
            } else {
                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dip2px(getContext(), 5),
                        dip2px(getContext(), 5),
                        dip2px(getContext(), 5),
                        dip2px(getContext(), 5));
                tagViewContainer.setLayoutParams(lp);
            }
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            tagView.setLayoutParams(lp);
            tagViewContainer.addView(tagView);
            addView(tagViewContainer);

            if (preCheckedList.contains(i)) {
                setChildChecked(i, tagViewContainer);
            }

            if (mTagAdapter.setSelected(i, adapter.getItem(i))) {
                setChildChecked(i, tagViewContainer);
            }

            tagView.setClickable(false);
            final TagView finalTagViewContainer = tagViewContainer;
            final int position = i;

            if (mTagClickable) {
                tagViewContainer.setOnClickListener(v -> {
                    doSelect(finalTagViewContainer, position);
                    if (mOnTagClickListener != null) {
                        mOnTagClickListener.onTagClick(finalTagViewContainer, position,
                                TagFlowLayout.this);
                    }
                });
            }
        }
        mSelectedView.addAll(preCheckedList);
    }

    public void setMaxSelectCount(int count) {
        if (mSelectedView.size() > count) {
            mSelectedView.clear();
        }
        mSelectedMax = count;
    }

    public Set<Integer> getSelectedList() {
        return new HashSet<>(mSelectedView);
    }

    private void setChildChecked(int position, TagView view) {
        view.setChecked(true);
        mTagAdapter.onSelected(position, view.getTagView());
    }

    private void setChildUnChecked(int position, TagView view) {
        view.setChecked(false);
        mTagAdapter.unSelected(position, view.getTagView());
    }

    private void doSelect(TagView child, int position) {
        if (!child.isChecked()) {
            //处理max_select=1的情况
            if (mSelectedMax == 1 && mSelectedView.size() == 1) {
                Iterator<Integer> iterator = mSelectedView.iterator();
                Integer preIndex = iterator.next();
                TagView pre = (TagView) getChildAt(preIndex);
                setChildUnChecked(preIndex, pre);
                setChildChecked(position, child);

                mSelectedView.remove(preIndex);
                mSelectedView.add(position);
            } else {
                if (mSelectedMax > 0 && mSelectedView.size() >= mSelectedMax) {
                    return;
                }
                setChildChecked(position, child);
                mSelectedView.add(position);
            }
        } else {
            setChildUnChecked(position, child);
            mSelectedView.remove(position);
        }
        if (mOnSelectListener != null) {
            mOnSelectListener.onSelected(new HashSet<Integer>(mSelectedView));
        }
    }

    public TagAdapter getAdapter() {
        return mTagAdapter;
    }

    private static final String KEY_CHOOSE_POS = "key_choose_pos";
    private static final String KEY_DEFAULT = "key_default";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT, super.onSaveInstanceState());

        String selectPos = "";
        if (mSelectedView.size() > 0) {
            for (int key : mSelectedView) {
                selectPos += key + "|";
            }
            selectPos = selectPos.substring(0, selectPos.length() - 1);
        }
        bundle.putString(KEY_CHOOSE_POS, selectPos);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            String mSelectPos = bundle.getString(KEY_CHOOSE_POS);
            if (!TextUtils.isEmpty(mSelectPos)) {
                String[] split = mSelectPos.split("\\|");
                for (String pos : split) {
                    int index = Integer.parseInt(pos);
                    mSelectedView.add(index);

                    TagView tagView = (TagView) getChildAt(index);
                    if (tagView != null) {
                        setChildChecked(index, tagView);
                    }
                }
            }
            super.onRestoreInstanceState(bundle.getParcelable(KEY_DEFAULT));
            return;
        }
        super.onRestoreInstanceState(state);
    }


    @Override
    public void onChanged() {
        mSelectedView.clear();
        changeAdapter();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}

