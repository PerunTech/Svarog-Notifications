import { React, connect, PropTypes, GenericGrid, ComponentManager } from 'perun-core'
import ArchiveMessages from './ArchiveMessages';
const tableName = 'SUBJECT'

class ArchiveComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      objIdState: '',
      objectTypeState: '',
      showGrid: false,
      gridId: `${tableName}_ARCHIVED_GRID`,
    };

  }

  componentDidMount() {
    this.showArchiveGrid();
  }

  componentWillUnmount() {
    ComponentManager.cleanComponentReducerState(this.state.gridId)
  }

  showArchiveGrid = () => {
    const { gridId } = this.state
    let gridElementArr = []
    let htmlElement = <div className='context-menu-holder'>
      <p className='archive-paragraph'>Archived</p>
    </div>
    let grid = <GenericGrid
      gridType={'READ_URL'}
      key={gridId}
      id={gridId}
      configTableName={'/SvarogNotificationsServices/getTableFieldList/%session/' + tableName}
      dataTableName={'/SvarogNotificationsServices/getArchivedSubjects/%session'}
      minHeight={600}
      onRowClickFunct={this.onArchiveRowClick}
      customClassName={'customGridClass'}
    />
    gridElementArr.push(htmlElement, grid)

    this.setState({ generateGridElement: gridElementArr, showGrid: true })
    ComponentManager.cleanComponentReducerState(gridId)
  }

  onArchiveRowClick = (id, idx, row) => {
    // const objId = `id=${row[`${tableName}.OBJECT_ID`]}`
    // const objectType = `type=${row[`${tableName}.OBJECT_TYPE`]}`
    const objId = row[`${tableName}.OBJECT_ID`]
    const objectType = row[`${tableName}.OBJECT_TYPE`]
    this.setState({ objIdState: objId, objectTypeState: objectType, showGrid: false })
    // hashHistory.push(`/main/message?${objId}&${objectType}`)
  }

  handleBack = () => {
    this.setState({ objIdState: '', objectTypeState: '', showGrid: true })
  }

  render() {
    const { generateGridElement, objIdState, objectTypeState, showGrid } = this.state
    return (
      <React.Fragment>
        {objIdState && objectTypeState && <ArchiveMessages handleBack={this.handleBack} objId={objIdState} objType={objectTypeState} />}
        {showGrid && generateGridElement}
      </React.Fragment>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

ArchiveComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(ArchiveComponent)