import { describe, expect, it, vi } from 'vitest';
import FailureGridCell from '@/components/FailureGridCell.vue';
import { shallowMount } from '@vue/test-utils';
import GridCell from '@/components/GridCell.vue';

describe('<FailureGridCell />', () => {
  vi.mock('@/services/utils', () => {
    return {
      getRelativeTime: vi.fn().mockReturnValue('10 months ago')
    };
  });

  it('should render grid cell', () => {
    const createdAt = '2022-08-15T02:20:34Z';

    const name = 'test name';
    const url = 'https://test.com';
    const wrapper = shallowMount(FailureGridCell, {
      props: {
        content: {
          id: '1234',
          name,
          url,
          createdAt
        }
      }
    });

    const gridCellComponent = wrapper.findComponent(GridCell);

    expect(gridCellComponent.exists()).toBeTruthy();
    expect(gridCellComponent.props()).toEqual({
      displayToggleVisibility: false,
      hidden: false,
      inProgress: false,
      lastExecutedTime: createdAt,
      name,
      showRelativeTime: true,
      status: 'failure',
      url
    });
  });
});
