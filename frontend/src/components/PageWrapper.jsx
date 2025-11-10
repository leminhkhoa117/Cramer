import { motion } from 'framer-motion';

const PageWrapper = ({ children }) => (
  <motion.div
    initial={{ opacity: 0, y: 15 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: 15 }}
    transition={{ duration: 0.25 }}
  >
    {children}
  </motion.div>
);

export default PageWrapper;
