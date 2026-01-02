USE collab_edit;

INSERT INTO tags (name, created_by, created_at) VALUES
('General', NULL, NOW()),
('Product', NULL, NOW()),
('Design', NULL, NOW());

INSERT INTO doc_templates (name, content, created_at) VALUES
('Meeting Notes', '<h2>Meeting Notes</h2><p>Attendees:</p><ul><li></li></ul><p>Agenda:</p><ol><li></li></ol><p>Decisions:</p><p></p>', NOW()),
('Product Spec', '<h2>Product Spec</h2><p>Summary:</p><p></p><h3>Goals</h3><ul><li></li></ul><h3>Requirements</h3><ul><li></li></ul>', NOW()),
('Research', '<h2>Research</h2><p>Background:</p><p></p><h3>Findings</h3><ul><li></li></ul><h3>Next steps</h3><p></p>', NOW());
