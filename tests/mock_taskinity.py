"""
Mock implementations of Taskinity modules for testing.
This allows tests to run without requiring all dependencies.
"""
from unittest.mock import MagicMock
import sys

# Create mock modules
mock_taskinity = MagicMock()
mock_core = MagicMock()
mock_extensions = MagicMock()
mock_utils = MagicMock()

# Set up the task decorator to pass through the function
def mock_task(*args, **kwargs):
    def decorator(func):
        return func
    return decorator

# Set up the flow decorator to pass through the function
def mock_flow(*args, **kwargs):
    def decorator(func):
        return func
    return decorator

# Set up run_flow_from_dsl to return a mock result
def mock_run_flow_from_dsl(flow_dsl, input_data):
    return {
        "status": "success",
        "processed_emails": 5,
        "urgent_emails": 1,
        "attachment_emails": 2,
        "regular_emails": 2
    }

# Set up mock functions
mock_core.taskinity_core.task = mock_task
mock_core.taskinity_core.flow = mock_flow
mock_core.taskinity_core.run_flow_from_dsl = mock_run_flow_from_dsl
mock_core.taskinity_core.save_dsl = MagicMock()
mock_core.taskinity_core.load_dsl = MagicMock(return_value="flow test {}")

# Add the mocks to the mock taskinity module
mock_taskinity.core = mock_core
mock_taskinity.extensions = mock_extensions
mock_taskinity.utils = mock_utils

# Mock other commonly used modules
sys.modules['taskinity'] = mock_taskinity
sys.modules['taskinity.core'] = mock_core
sys.modules['taskinity.core.taskinity_core'] = mock_core.taskinity_core
sys.modules['taskinity.extensions'] = mock_extensions
sys.modules['taskinity.utils'] = mock_utils

# Mock other dependencies that might be imported
sys.modules['gql'] = MagicMock()
sys.modules['schedule'] = MagicMock()
