import { useEffect, useState } from 'react'
import {
  Stack, Title, Text, Alert, Loader, Center, Paper, Group, Button,
  ActionIcon, TextInput, Tooltip,
} from '@mantine/core'
import { IconAlertCircle, IconPlus, IconTrash, IconDeviceFloppy } from '@tabler/icons-react'
import { getAvailability, updateAvailability, ApiError } from '../../api/client'
import type { AvailabilityConfig, WeeklyAvailability, DateOverride, TimeRange, DayOfWeek } from '../../api/client'

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'monday', label: 'Monday' },
  { value: 'tuesday', label: 'Tuesday' },
  { value: 'wednesday', label: 'Wednesday' },
  { value: 'thursday', label: 'Thursday' },
  { value: 'friday', label: 'Friday' },
  { value: 'saturday', label: 'Saturday' },
  { value: 'sunday', label: 'Sunday' },
]

function emptyTimeRange(): TimeRange {
  return { start: '09:00', end: '17:00' }
}

export default function AvailabilityPage() {
  const [config, setConfig] = useState<AvailabilityConfig | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    getAvailability()
      .then(setConfig)
      .catch((err) => {
        if (err instanceof ApiError) {
          setError(err.body.message)
        } else {
          setError('Failed to load availability')
        }
      })
      .finally(() => setLoading(false))
  }, [])

  function updateWeekly(index: number, update: Partial<WeeklyAvailability>) {
    if (!config) return
    const weekly = config.weekly.map((w, i) => i === index ? { ...w, ...update } : w)
    setConfig({ ...config, weekly })
  }

  function removeWindow(dayIndex: number, windowIndex: number) {
    if (!config) return
    const day = config.weekly[dayIndex]
    const windows = day.windows.filter((_, i) => i !== windowIndex)
    updateWeekly(dayIndex, { windows })
  }

  function addWindow(dayIndex: number) {
    if (!config) return
    const day = config.weekly[dayIndex]
    updateWeekly(dayIndex, { windows: [...day.windows, emptyTimeRange()] })
  }

  function updateWindow(dayIndex: number, windowIndex: number, field: 'start' | 'end', value: string) {
    if (!config) return
    const day = config.weekly[dayIndex]
    const windows = day.windows.map((w, i) =>
      i === windowIndex ? { ...w, [field]: value } : w
    )
    updateWeekly(dayIndex, { windows })
  }

  function updateOverride(index: number, update: Partial<DateOverride>) {
    if (!config) return
    const overrides = config.overrides.map((o, i) => i === index ? { ...o, ...update } : o)
    setConfig({ ...config, overrides })
  }

  function removeOverride(index: number) {
    if (!config) return
    setConfig({ ...config, overrides: config.overrides.filter((_, i) => i !== index) })
  }

  function addOverride() {
    if (!config) return
    setConfig({
      ...config,
      overrides: [...config.overrides, { date: '', windows: [emptyTimeRange()] }],
    })
  }

  function addOverrideWindow(overrideIndex: number) {
    if (!config) return
    const override = config.overrides[overrideIndex]
    const windows = override.windows ?? []
    updateOverride(overrideIndex, { windows: [...windows, emptyTimeRange()] })
  }

  function removeOverrideWindow(overrideIndex: number, windowIndex: number) {
    if (!config) return
    const override = config.overrides[overrideIndex]
    const windows = (override.windows ?? []).filter((_, i) => i !== windowIndex)
    updateOverride(overrideIndex, { windows: windows.length > 0 ? windows : undefined })
  }

  function updateOverrideWindow(overrideIndex: number, windowIndex: number, field: 'start' | 'end', value: string) {
    if (!config) return
    const override = config.overrides[overrideIndex]
    const windows = (override.windows ?? []).map((w, i) =>
      i === windowIndex ? { ...w, [field]: value } : w
    )
    updateOverride(overrideIndex, { windows })
  }

  function toggleUnavailable(overrideIndex: number) {
    if (!config) return
    const override = config.overrides[overrideIndex]
    if (override.windows === undefined) {
      updateOverride(overrideIndex, { windows: [emptyTimeRange()] })
    } else {
      updateOverride(overrideIndex, { windows: undefined })
    }
  }

  async function handleSave() {
    if (!config) return
    setSaving(true)
    setError(null)
    setSuccess(false)
    try {
      await updateAvailability(config)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.body.message)
      } else {
        setError('Failed to save availability')
      }
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Center h={200}>
        <Loader />
      </Center>
    )
  }

  if (!config) {
    return error ? (
      <Alert icon={<IconAlertCircle size={16} />} color="red" variant="light">{error}</Alert>
    ) : null
  }

  return (
    <Stack>
      <Title order={2}>Availability Settings</Title>

      {error && (
        <Alert icon={<IconAlertCircle size={16} />} color="red" variant="light">{error}</Alert>
      )}
      {success && (
        <Alert color="green" variant="light">Availability settings saved successfully</Alert>
      )}

      <Paper shadow="sm" p="md" radius="md" withBorder>
        <Title order={4} mb="md">Weekly Availability</Title>
        {config.weekly.map((day, dayIndex) => (
          <Paper key={day.dayOfWeek} withBorder p="sm" mb="sm" radius="sm">
            <Group mb="xs">
              <Text fw={500} style={{ width: 100 }}>{DAYS.find(d => d.value === day.dayOfWeek)?.label}</Text>
              <Button size="xs" variant="subtle" leftSection={<IconPlus size={14} />} onClick={() => addWindow(dayIndex)}>
                Add window
              </Button>
            </Group>
            {day.windows.map((window, winIndex) => (
              <Group key={winIndex} mb="xs">
                <TextInput
                  label="Start"
                  value={window.start}
                  onChange={(e) => updateWindow(dayIndex, winIndex, 'start', e.currentTarget.value)}
                  style={{ width: 100 }}
                  size="sm"
                />
                <TextInput
                  label="End"
                  value={window.end}
                  onChange={(e) => updateWindow(dayIndex, winIndex, 'end', e.currentTarget.value)}
                  style={{ width: 100 }}
                  size="sm"
                />
                <Tooltip label="Remove window">
                  <ActionIcon color="red" variant="light" onClick={() => removeWindow(dayIndex, winIndex)} mt="xl">
                    <IconTrash size={16} />
                  </ActionIcon>
                </Tooltip>
              </Group>
            ))}
          </Paper>
        ))}
      </Paper>

      <Paper shadow="sm" p="md" radius="md" withBorder>
        <Group mb="md">
          <Title order={4}>Date Overrides</Title>
          <Button size="xs" variant="subtle" leftSection={<IconPlus size={14} />} onClick={addOverride}>
            Add override
          </Button>
        </Group>
        {config.overrides.length === 0 && (
          <Text c="dimmed" size="sm">No date overrides configured</Text>
        )}
        {config.overrides.map((override, oi) => (
          <Paper key={oi} withBorder p="sm" mb="sm" radius="sm">
            <Group mb="xs">
              <TextInput
                label="Date (YYYY-MM-DD)"
                value={override.date}
                onChange={(e) => updateOverride(oi, { date: e.currentTarget.value })}
                style={{ width: 200 }}
                size="sm"
              />
              <Button
                size="xs"
                variant={override.windows === undefined ? 'filled' : 'subtle'}
                color="red"
                onClick={() => toggleUnavailable(oi)}
                mt="xl"
              >
                {override.windows === undefined ? 'Unavailable all day' : 'Mark as unavailable'}
              </Button>
              <ActionIcon color="red" variant="light" onClick={() => removeOverride(oi)} mt="xl">
                <IconTrash size={16} />
              </ActionIcon>
            </Group>
            {override.windows !== undefined && (
              <>
                {(override.windows ?? []).map((window, wi) => (
                  <Group key={wi} mb="xs" ml="md">
                    <TextInput
                      label="Start"
                      value={window.start}
                      onChange={(e) => updateOverrideWindow(oi, wi, 'start', e.currentTarget.value)}
                      style={{ width: 100 }}
                      size="sm"
                    />
                    <TextInput
                      label="End"
                      value={window.end}
                      onChange={(e) => updateOverrideWindow(oi, wi, 'end', e.currentTarget.value)}
                      style={{ width: 100 }}
                      size="sm"
                    />
                    <Tooltip label="Remove window">
                      <ActionIcon color="red" variant="light" onClick={() => removeOverrideWindow(oi, wi)} mt="xl">
                        <IconTrash size={16} />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                ))}
                <Button size="xs" variant="subtle" leftSection={<IconPlus size={14} />} ml="md" onClick={() => addOverrideWindow(oi)}>
                  Add window
                </Button>
              </>
            )}
          </Paper>
        ))}
      </Paper>

      <Group>
        <Button leftSection={<IconDeviceFloppy size={16} />} onClick={handleSave} loading={saving}>
          Save Availability
        </Button>
      </Group>
    </Stack>
  )
}
