import pytest
import sys
import os

# Add the parent directory to the path so we can import the modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import our mock modules first to ensure they're loaded before any tests
from mock_taskinity import mock_taskinity

# Add fixtures here if needed
@pytest.fixture
def sample_emails():
    """Fixture providing sample email data for tests."""
    return [
        {
            'id': '1',
            'subject': 'URGENT: Meeting',
            'body': 'Please attend the meeting tomorrow',
            'from': 'boss@example.com',
            'date': '2025-05-24T10:00:00',
            'has_attachment': False
        },
        {
            'id': '2',
            'subject': 'Report',
            'body': 'Please find the attached report',
            'from': 'colleague@example.com',
            'date': '2025-05-24T09:30:00',
            'has_attachment': True
        },
        {
            'id': '3',
            'subject': 'Newsletter',
            'body': 'Latest company news',
            'from': 'news@example.com',
            'date': '2025-05-24T08:00:00',
            'has_attachment': False
        }
    ]

@pytest.fixture
def classified_emails(sample_emails):
    """Fixture providing pre-classified email data."""
    return {
        'urgent': [sample_emails[0]],
        'with_attachments': [sample_emails[1]],
        'regular': [sample_emails[2]]
    }
