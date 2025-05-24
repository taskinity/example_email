import pytest
from unittest.mock import patch, MagicMock
import sys
import os

# Add the parent directory to the path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import our mock Taskinity module first
from mock_taskinity import mock_run_flow_from_dsl

# Now we can safely import flow
import flow

class TestEmailFlow:
    """Test suite for the email processing flow."""
    
    def test_flow_execution(self):
        """Test that the flow can be executed."""
        # This test simply verifies that the flow.py file exists and can be imported
        # We've already imported it at the top of the file, so if that worked, this test passes
        assert hasattr(flow, 'main'), "flow.py should have a main function"
        
        # We can also check that the flow.dsl file exists
        import os
        flow_dsl_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'flow.dsl')
        assert os.path.exists(flow_dsl_path), "flow.dsl file should exist"
    
    def test_dsl_flow_structure(self):
        """Test that the DSL flow has the expected structure."""
        # Read the flow DSL from the file
        with open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'flow.dsl'), 'r') as f:
            flow_dsl = f.read()
        
        # Check that the flow contains the expected tasks
        assert 'fetch_emails' in flow_dsl
        assert 'classify_emails' in flow_dsl
        assert 'process_urgent_emails' in flow_dsl
        assert 'process_emails_with_attachments' in flow_dsl
        assert 'process_regular_emails' in flow_dsl
        assert 'send_responses' in flow_dsl
